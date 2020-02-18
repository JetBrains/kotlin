/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.coroutines.DEBUG_METADATA_ANNOTATION_ASM_TYPE
import org.jetbrains.kotlin.codegen.coroutines.isCoroutineSuperClass
import org.jetbrains.kotlin.codegen.coroutines.isResumeImplMethodName
import org.jetbrains.kotlin.codegen.inline.coroutines.CoroutineTransformer
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.serialization.JvmCodegenStringTable
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapperBase
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.header.ReadKotlinClassHeaderAnnotationVisitor
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.Companion.NO_ORIGIN
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.*

class AnonymousObjectTransformer(
    transformationInfo: AnonymousObjectTransformationInfo,
    private val inliningContext: InliningContext,
    private val isSameModule: Boolean,
    private val continuationClassName: String?
) : ObjectTransformer<AnonymousObjectTransformationInfo>(transformationInfo, inliningContext.state) {

    private val oldObjectType = Type.getObjectType(transformationInfo.oldClassName)

    private val fieldNames = hashMapOf<String, MutableList<String>>()

    private var constructor: MethodNode? = null
    private lateinit var sourceMap: SMAP
    private lateinit var sourceMapper: SourceMapper
    private val languageVersionSettings = inliningContext.state.languageVersionSettings

    // TODO: use IrTypeMapper in the IR backend
    private val typeMapper: KotlinTypeMapperBase = state.typeMapper

    override fun doTransform(parentRemapper: FieldRemapper): InlineResult {
        val innerClassNodes = ArrayList<InnerClassNode>()
        val classBuilder = createRemappingClassBuilderViaFactory(inliningContext)
        val methodsToTransform = ArrayList<MethodNode>()
        val metadataReader = ReadKotlinClassHeaderAnnotationVisitor()
        lateinit var superClassName: String
        var sourceInfo: String? = null
        var debugInfo: String? = null
        var debugMetadataAnnotation: AnnotationNode? = null

        createClassReader().accept(object : ClassVisitor(Opcodes.API_VERSION, classBuilder.visitor) {
            override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<String>) {
                classBuilder.defineClass(null, maxOf(version, state.classFileVersion), access, name, signature, superName, interfaces)
                if (languageVersionSettings.isCoroutineSuperClass(superName)) {
                    inliningContext.isContinuation = true
                }
                superClassName = superName
            }

            override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
                innerClassNodes.add(InnerClassNode(name, outerName, innerName, access))
            }

            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                if (desc == JvmAnnotationNames.METADATA_DESC) {
                    // Empty inner class info because no inner classes are used in kotlin.Metadata and its arguments
                    val innerClassesInfo = FileBasedKotlinClass.InnerClassesInfo()
                    return FileBasedKotlinClass.convertAnnotationVisitor(metadataReader, desc, innerClassesInfo)
                } else if (desc == DEBUG_METADATA_ANNOTATION_ASM_TYPE.descriptor) {
                    debugMetadataAnnotation = AnnotationNode(desc)
                    return debugMetadataAnnotation
                }
                return super.visitAnnotation(desc, visible)
            }

            override fun visitMethod(
                access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?
            ): MethodVisitor {
                val node = MethodNode(access, name, desc, signature, exceptions)
                if (name == "<init>") {
                    if (constructor != null) {
                        throw RuntimeException("Lambda, SAM or anonymous object should have only one constructor")
                    }
                    constructor = node
                } else {
                    methodsToTransform.add(node)
                }
                return node
            }

            override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                addUniqueField(name)
                return if (isCapturedFieldName(name)) {
                    null
                } else {
                    classBuilder.newField(JvmDeclarationOrigin.NO_ORIGIN, access, name, desc, signature, value)
                }
            }

            override fun visitSource(source: String, debug: String?) {
                sourceInfo = source
                debugInfo = debug
            }

            override fun visitEnd() {}
        }, ClassReader.SKIP_FRAMES)

        // When regenerating objects in inline lambdas, keep the old SMAP and don't remap the line numbers to
        // save time. The result is effectively the same anyway.
        val debugInfoToParse = if (inliningContext.isInliningLambda) null else debugInfo
        val (firstLine, lastLine) = (methodsToTransform + listOfNotNull(constructor)).lineNumberRange()
        sourceMap = SMAPParser.parseOrCreateDefault(debugInfoToParse, sourceInfo, oldObjectType.internalName, firstLine, lastLine)
        sourceMapper = SourceMapper(sourceMap.fileMappings.firstOrNull { it.name == sourceInfo }?.toSourceInfo())

        val allCapturedParamBuilder = ParametersBuilder.newBuilder()
        val constructorParamBuilder = ParametersBuilder.newBuilder()
        val additionalFakeParams = extractParametersMappingAndPatchConstructor(
            constructor!!, allCapturedParamBuilder, constructorParamBuilder, transformationInfo, parentRemapper
        )

        val deferringMethods = ArrayList<DeferredMethodVisitor>()

        generateConstructorAndFields(classBuilder, allCapturedParamBuilder, constructorParamBuilder, parentRemapper, additionalFakeParams)

        val coroutineTransformer = CoroutineTransformer(
            inliningContext,
            classBuilder,
            methodsToTransform,
            superClassName
        )
        var putDebugMetadata = false
        loop@ for (next in methodsToTransform) {
            val deferringVisitor =
                when {
                    coroutineTransformer.shouldSkip(next) -> continue@loop
                    coroutineTransformer.shouldGenerateStateMachine(next) -> coroutineTransformer.newMethod(next)
                    else -> {
                        // Debug metadata is not put, but we should keep, since we do not generate state-machine,
                        // if the lambda does not capture crossinline lambdas.
                        if (coroutineTransformer.suspendLambdaWithGeneratedStateMachine(next)) {
                            putDebugMetadata = true
                        }
                        newMethod(classBuilder, next)
                    }
                }

            if (next.name == "<clinit>") {
                rewriteAssertionsDisabledFieldInitialization(next, inliningContext.root.callSiteInfo.ownerClassName)
            }

            val funResult = inlineMethodAndUpdateGlobalResult(parentRemapper, deferringVisitor, next, allCapturedParamBuilder, false)

            val returnType = Type.getReturnType(next.desc)
            if (!AsmUtil.isPrimitive(returnType)) {
                val oldFunReturnType = returnType.internalName
                val newFunReturnType = funResult.getChangedTypes()[oldFunReturnType]
                if (newFunReturnType != null) {
                    inliningContext.typeRemapper.addAdditionalMappings(oldFunReturnType, newFunReturnType)
                }
            }
            deferringMethods.add(deferringVisitor)
        }

        deferringMethods.forEach { method ->
            val continuationToRemove = CoroutineTransformer.findFakeContinuationConstructorClassName(method.intermediate)
            val oldContinuationName = coroutineTransformer.oldContinuationFrom(method.intermediate)
            coroutineTransformer.replaceFakesWithReals(method.intermediate)
            removeFinallyMarkers(method.intermediate)
            method.visitEnd()
            if (continuationToRemove != null && coroutineTransformer.safeToRemoveContinuationClass(method.intermediate)) {
                transformationResult.addClassToRemove(continuationToRemove)
                innerClassNodes.removeIf { it.name == oldContinuationName }
            }
        }

        if (GENERATE_SMAP && !inliningContext.isInliningLambda) {
            classBuilder.visitSMAP(sourceMapper, !state.languageVersionSettings.supportsFeature(LanguageFeature.CorrectSourceMappingSyntax))
        } else if (sourceInfo != null) {
            classBuilder.visitSource(sourceInfo!!, debugInfo)
        }

        val visitor = classBuilder.visitor
        innerClassNodes.forEach { node ->
            visitor.visitInnerClass(node.name, node.outerName, node.innerName, node.access)
        }

        val header = metadataReader.createHeader()
        if (header != null) {
            writeTransformedMetadata(header, classBuilder)
        }

        // debugMetadataAnnotation can be null in LV < 1.3
        if (putDebugMetadata && debugMetadataAnnotation != null) {
            visitor.visitAnnotation(debugMetadataAnnotation!!.desc, true).also {
                debugMetadataAnnotation!!.accept(it)
            }
        }

        writeOuterInfo(visitor)

        if (inliningContext.generateAssertField && fieldNames.none { it.key == ASSERTIONS_DISABLED_FIELD_NAME }) {
            val clInitBuilder = classBuilder.newMethod(NO_ORIGIN, Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)
            generateAssertionsDisabledFieldInitialization(classBuilder, clInitBuilder, inliningContext.root.callSiteInfo.ownerClassName)
            clInitBuilder.visitInsn(Opcodes.RETURN)
            clInitBuilder.visitEnd()
        }

        if (continuationClassName == transformationInfo.oldClassName) {
            coroutineTransformer.registerClassBuilder(continuationClassName)
        } else {
            classBuilder.done()
        }

        return transformationResult
    }

    private fun writeTransformedMetadata(header: KotlinClassHeader, classBuilder: ClassBuilder) {
        writeKotlinMetadata(classBuilder, state, header.kind, header.extraInt) action@{ av ->
            val (newProto, newStringTable) = transformMetadata(header) ?: run {
                val data = header.data
                val strings = header.strings
                if (data != null && strings != null) {
                    AsmUtil.writeAnnotationData(av, data, strings)
                }
                return@action
            }
            DescriptorAsmUtil.writeAnnotationData(av, newProto, newStringTable)
        }
    }

    private fun transformMetadata(header: KotlinClassHeader): Pair<MessageLite, JvmStringTable>? {
        val data = header.data ?: return null
        val strings = header.strings ?: return null

        when (header.kind) {
            KotlinClassHeader.Kind.CLASS -> {
                val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(data, strings)
                val newStringTable = JvmCodegenStringTable(typeMapper, nameResolver)
                val newProto = classProto.toBuilder().apply {
                    setExtension(JvmProtoBuf.anonymousObjectOriginName, newStringTable.getStringIndex(oldObjectType.internalName))
                }.build()
                return newProto to newStringTable
            }
            KotlinClassHeader.Kind.SYNTHETIC_CLASS -> {
                val (nameResolver, functionProto) = JvmProtoBufUtil.readFunctionDataFrom(data, strings)
                val newStringTable = JvmCodegenStringTable(typeMapper, nameResolver)
                val newProto = functionProto.toBuilder().apply {
                    setExtension(JvmProtoBuf.lambdaClassOriginName, newStringTable.getStringIndex(oldObjectType.internalName))
                }.build()
                return newProto to newStringTable
            }
            else -> return null
        }
    }

    private fun writeOuterInfo(visitor: ClassVisitor) {
        val info = inliningContext.callSiteInfo
        // Since $$forInline functions are not generated if retransformation is the last one (i.e. call site is not inline)
        // link to the function in OUTERCLASS field becomes invalid. However, since $$forInline function always has no-inline
        // companion without the suffix, use it.
        visitor.visitOuterClass(info.ownerClassName, info.functionName?.removeSuffix(FOR_INLINE_SUFFIX), info.functionDesc)
    }

    private fun inlineMethodAndUpdateGlobalResult(
        parentRemapper: FieldRemapper,
        deferringVisitor: MethodVisitor,
        next: MethodNode,
        allCapturedParamBuilder: ParametersBuilder,
        isConstructor: Boolean
    ): InlineResult {
        val funResult = inlineMethod(parentRemapper, deferringVisitor, next, allCapturedParamBuilder, isConstructor)
        transformationResult.merge(funResult)
        transformationResult.reifiedTypeParametersUsages.mergeAll(funResult.reifiedTypeParametersUsages)
        return funResult
    }

    private fun inlineMethod(
        parentRemapper: FieldRemapper,
        deferringVisitor: MethodVisitor,
        sourceNode: MethodNode,
        capturedBuilder: ParametersBuilder,
        isConstructor: Boolean
    ): InlineResult {
        val typeParametersToReify = inliningContext.root.inlineMethodReifier.reifyInstructions(sourceNode)
        val parameters =
            if (isConstructor) capturedBuilder.buildParameters() else getMethodParametersWithCaptured(capturedBuilder, sourceNode)

        val remapper = RegeneratedLambdaFieldRemapper(
            oldObjectType.internalName, transformationInfo.newClassName, parameters,
            transformationInfo.capturedLambdasToInline, parentRemapper, isConstructor
        )

        val inliner = MethodInliner(
            sourceNode,
            parameters,
            inliningContext.subInline(transformationInfo.nameGenerator),
            remapper,
            isSameModule,
            "Transformer for " + transformationInfo.oldClassName,
            SourceMapCopier(sourceMapper, sourceMap),
            InlineCallSiteInfo(
                transformationInfo.oldClassName,
                sourceNode.name,
                if (isConstructor) transformationInfo.newConstructorDescriptor else sourceNode.desc,
                inliningContext.callSiteInfo.isInlineOrInsideInline,
                isSuspendFunctionOrLambda(sourceNode),
                inliningContext.root.sourceCompilerForInline.inlineCallSiteInfo.lineNumber
            ), null
        )

        val result = inliner.doInline(deferringVisitor, LocalVarRemapper(parameters, 0), false, mapOf())
        result.reifiedTypeParametersUsages.mergeAll(typeParametersToReify)
        deferringVisitor.visitMaxs(-1, -1)
        return result
    }

    private fun isSuspendFunctionOrLambda(sourceNode: MethodNode): Boolean =
        (sourceNode.desc.endsWith(";Lkotlin/coroutines/Continuation;)Ljava/lang/Object;") ||
                sourceNode.desc.endsWith(";Lkotlin/coroutines/experimental/Continuation;)Ljava/lang/Object;")) &&
                (CoroutineTransformer.findFakeContinuationConstructorClassName(sourceNode) != null ||
                        languageVersionSettings.isResumeImplMethodName(sourceNode.name.removeSuffix(FOR_INLINE_SUFFIX)))

    private fun generateConstructorAndFields(
        classBuilder: ClassBuilder,
        allCapturedBuilder: ParametersBuilder,
        constructorInlineBuilder: ParametersBuilder,
        parentRemapper: FieldRemapper,
        constructorAdditionalFakeParams: List<CapturedParamInfo>
    ) {
        val descTypes = ArrayList<Type>()

        val constructorParams = constructorInlineBuilder.buildParameters()
        val capturedIndexes = IntArray(constructorParams.parameters.size)
        var index = 0
        var size = 0

        //complex processing cause it could have super constructor call params
        for (info in constructorParams) {
            if (!info.isSkipped) { //not inlined
                if (info.isCaptured || info is CapturedParamInfo) {
                    capturedIndexes[index] = size
                    index++
                }

                if (size != 0) { //skip this
                    descTypes.add(info.type)
                }
                size += info.type.size
            }
        }

        val constructorDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, *descTypes.toTypedArray())
        //TODO for inline method make public class
        transformationInfo.newConstructorDescriptor = constructorDescriptor
        val constructorVisitor = classBuilder.newMethod(
            NO_ORIGIN, constructor!!.access, "<init>", constructorDescriptor, null, ArrayUtil.EMPTY_STRING_ARRAY
        )

        val newBodyStartLabel = Label()
        constructorVisitor.visitLabel(newBodyStartLabel)
        //initialize captured fields
        val newFieldsWithSkipped = getNewFieldsToGenerate(allCapturedBuilder.listCaptured())
        val fieldInfoWithSkipped = transformToFieldInfo(Type.getObjectType(transformationInfo.newClassName), newFieldsWithSkipped)

        val capturedFieldInitializer = InstructionAdapter(constructorVisitor)
        fieldInfoWithSkipped.forEachIndexed { paramIndex, fieldInfo ->
            if (!newFieldsWithSkipped[paramIndex].skip) {
                DescriptorAsmUtil.genAssignInstanceFieldFromParam(fieldInfo, capturedIndexes[paramIndex], capturedFieldInitializer)
            }
        }

        //then transform constructor
        //HACK: in inlining into constructor we access original captured fields with field access not local var
        //but this fields added to general params (this assumes local var access) not captured one,
        //so we need to add them to captured params
        for (info in constructorAdditionalFakeParams) {
            val fake = constructorInlineBuilder.addCapturedParamCopy(info)

            if (fake.functionalArgument is LambdaInfo) {
                //set remap value to skip this fake (captured with lambda already skipped)
                val composed = StackValue.field(
                    fake.type,
                    oldObjectType,
                    fake.newFieldName,
                    false,
                    StackValue.LOCAL_0
                )
                fake.remapValue = composed
            }
        }

        val intermediateMethodNode = MethodNode(constructor!!.access, "<init>", constructorDescriptor, null, ArrayUtil.EMPTY_STRING_ARRAY)
        inlineMethodAndUpdateGlobalResult(parentRemapper, intermediateMethodNode, constructor!!, constructorInlineBuilder, true)
        removeFinallyMarkers(intermediateMethodNode)

        val first = intermediateMethodNode.instructions.first
        val oldStartLabel = (first as? LabelNode)?.label
        intermediateMethodNode.accept(object : MethodBodyVisitor(capturedFieldInitializer) {
            override fun visitLocalVariable(
                name: String, desc: String, signature: String?, start: Label, end: Label, index: Int
            ) {
                super.visitLocalVariable(
                    name, desc, signature,
                    //patch for jack&jill
                    if (oldStartLabel === start) newBodyStartLabel else start,
                    end, index
                )
            }
        })
        constructorVisitor.visitEnd()
        DescriptorAsmUtil.genClosureFields(
            toNameTypePair(filterSkipped(newFieldsWithSkipped)), classBuilder
        )
    }

    private fun getMethodParametersWithCaptured(capturedBuilder: ParametersBuilder, sourceNode: MethodNode): Parameters {
        val builder = ParametersBuilder.initializeBuilderFrom(
            oldObjectType,
            sourceNode.desc,
            isStatic = sourceNode.access and Opcodes.ACC_STATIC != 0
        )
        for (param in capturedBuilder.listCaptured()) {
            builder.addCapturedParamCopy(param)
        }
        return builder.buildParameters()
    }

    private fun newMethod(builder: ClassBuilder, original: MethodNode): DeferredMethodVisitor {
        return DeferredMethodVisitor(
            MethodNode(
                original.access, original.name, original.desc, original.signature,
                ArrayUtil.toStringArray(original.exceptions)
            )
        ) {
            builder.newMethod(
                NO_ORIGIN, original.access, original.name, original.desc, original.signature,
                ArrayUtil.toStringArray(original.exceptions)
            )
        }
    }

    private fun extractParametersMappingAndPatchConstructor(
        constructor: MethodNode,
        capturedParamBuilder: ParametersBuilder,
        constructorParamBuilder: ParametersBuilder,
        transformationInfo: AnonymousObjectTransformationInfo,
        parentFieldRemapper: FieldRemapper
    ): List<CapturedParamInfo> {
        val capturedLambdas = LinkedHashSet<LambdaInfo>() //captured var of inlined parameter
        val constructorAdditionalFakeParams = ArrayList<CapturedParamInfo>()
        val indexToFunctionalArgument = transformationInfo.functionalArguments
        val capturedParams = HashSet<Int>()

        //load captured parameters and patch instruction list
        //  NB: there is also could be object fields
        val toDelete = arrayListOf<AbstractInsnNode>()
        constructor.findCapturedFieldAssignmentInstructions().forEach { fieldNode ->
            val fieldName = fieldNode.name
            val parameterAload = fieldNode.previous as VarInsnNode
            val varIndex = parameterAload.`var`
            val functionalArgument = indexToFunctionalArgument[varIndex]
            val newFieldName = if (isThis0(fieldName) && shouldRenameThis0(parentFieldRemapper, indexToFunctionalArgument.values))
                getNewFieldName(fieldName, true)
            else
                fieldName
            val info = capturedParamBuilder.addCapturedParam(
                Type.getObjectType(transformationInfo.oldClassName), fieldName, newFieldName,
                Type.getType(fieldNode.desc), functionalArgument is LambdaInfo, null
            )
            info.functionalArgument = functionalArgument
            if (functionalArgument is LambdaInfo) {
                capturedLambdas.add(functionalArgument)
            }
            constructorAdditionalFakeParams.add(info)
            capturedParams.add(varIndex)

            toDelete.add(parameterAload.previous)
            toDelete.add(parameterAload)
            toDelete.add(fieldNode)
        }
        constructor.remove(toDelete)

        constructorParamBuilder.addThis(oldObjectType, false)

        val paramTypes = transformationInfo.constructorDesc?.let { Type.getArgumentTypes(it) } ?: emptyArray()
        for (type in paramTypes) {
            val info = indexToFunctionalArgument[constructorParamBuilder.nextParameterOffset]
            val parameterInfo = constructorParamBuilder.addNextParameter(type, info is LambdaInfo)
            parameterInfo.functionalArgument = info
            if (capturedParams.contains(parameterInfo.index)) {
                parameterInfo.isCaptured = true
            } else {
                //otherwise it's super constructor parameter
            }
        }

        //For all inlined lambdas add their captured parameters
        //TODO: some of such parameters could be skipped - we should perform additional analysis
        val allRecapturedParameters = ArrayList<CapturedParamDesc>()
        if (parentFieldRemapper !is InlinedLambdaRemapper || parentFieldRemapper.parent!!.isRoot) {
            // Possible cases:
            //
            //   1. Top-level object in an inline lambda that is *not* being inlined into another object. In this case, we
            //      have no choice but to add a separate field for each captured variable. `capturedLambdas` is either empty
            //      (already have the fields) or contains the parent lambda object (captures used to be read from it, but
            //      the object will be removed and its contents inlined).
            //
            //   2. Top-level object in a named inline function. Again, there's no option but to add separate fields.
            //      `capturedLambdas` contains all lambdas used by this object and nested objects.
            //
            //   3. Nested object, either in an inline lambda or an inline function. This case has two subcases:
            //      * The object's captures are passed as separate arguments (e.g. KT-28064 style object that used to be in a lambda);
            //        we could group them into `this$0` now, but choose not to. Lambdas are replaced by their captures.
            //      * The object's captures are already grouped into `this$0`; this includes captured lambda parameters (for objects in
            //        inline functions) and a reference to the outer object or lambda (for objects in lambdas), so `capturedLambdas` is
            //        empty and the choice doesn't matter.
            //
            val alreadyAdded = HashMap<String, CapturedParamInfo>()
            for (info in capturedLambdas) {
                for (desc in info.capturedVars) {
                    val key = desc.fieldName + "$$$" + desc.type.className
                    val alreadyAddedParam = alreadyAdded[key]

                    val recapturedParamInfo = capturedParamBuilder.addCapturedParam(
                        desc,
                        alreadyAddedParam?.newFieldName ?: getNewFieldName(desc.fieldName, false),
                        alreadyAddedParam != null
                    )
                    if (info is ExpressionLambda && info.isCapturedSuspend(desc)) {
                        recapturedParamInfo.functionalArgument = NonInlineableArgumentForInlineableParameterCalledInSuspend
                    }
                    val composed = StackValue.field(
                        desc.type,
                        oldObjectType, /*TODO owner type*/
                        recapturedParamInfo.newFieldName,
                        false,
                        StackValue.LOCAL_0
                    )
                    recapturedParamInfo.remapValue = composed
                    allRecapturedParameters.add(desc)

                    constructorParamBuilder.addCapturedParam(recapturedParamInfo, recapturedParamInfo.newFieldName).remapValue = composed

                    if (isThis0(desc.fieldName)) {
                        alreadyAdded.put(key, recapturedParamInfo)
                    }
                }
            }
        } else if (capturedLambdas.isNotEmpty()) {
            // Top-level object in a lambda inlined into another object. As already said above, either `capturedLambdas` is empty
            // (no captures or captures were generated as loose fields) or it contains a single entry for the parent lambda itself.
            // Simply replace one `this$0` (of lambda type) with another (of destination object type).
            val parent = parentFieldRemapper.parent as? RegeneratedLambdaFieldRemapper
                ?: throw AssertionError("Expecting RegeneratedLambdaFieldRemapper, but ${parentFieldRemapper.parent}")
            val ownerType = Type.getObjectType(parent.originalLambdaInternalName)
            val desc = CapturedParamDesc(ownerType, AsmUtil.THIS, ownerType)
            val recapturedParamInfo = capturedParamBuilder.addCapturedParam(desc, AsmUtil.CAPTURED_THIS_FIELD/*outer lambda/object*/, false)
            val composed = StackValue.LOCAL_0
            recapturedParamInfo.remapValue = composed
            allRecapturedParameters.add(desc)

            constructorParamBuilder.addCapturedParam(recapturedParamInfo, recapturedParamInfo.newFieldName).remapValue = composed
        }

        transformationInfo.allRecapturedParameters = allRecapturedParameters
        transformationInfo.capturedLambdasToInline = capturedLambdas.associateBy { it.lambdaClassType.internalName }

        return constructorAdditionalFakeParams
    }

    private fun shouldRenameThis0(parentFieldRemapper: FieldRemapper, values: Collection<FunctionalArgument>): Boolean {
        return if (isFirstDeclSiteLambdaFieldRemapper(parentFieldRemapper)) {
            values.any { it is LambdaInfo && it.capturedVars.any { isThis0(it.fieldName) } }
        } else false
    }

    private fun getNewFieldName(oldName: String, originalField: Boolean): String {
        if (AsmUtil.CAPTURED_THIS_FIELD == oldName) {
            return if (!originalField) {
                oldName
            } else {
                //rename original 'this$0' in declaration site lambda (inside inline function) to use this$0 only for outer lambda/object access on call site
                addUniqueField(oldName + INLINE_FUN_THIS_0_SUFFIX)
            }
        }
        return addUniqueField(oldName + INLINE_TRANSFORMATION_SUFFIX)
    }

    private fun addUniqueField(name: String): String {
        val existNames = fieldNames.getOrPut(name) { LinkedList() }
        val suffix = if (existNames.isEmpty()) "" else "$" + existNames.size
        val newName = name + suffix
        existNames.add(newName)
        return newName
    }

    private fun isFirstDeclSiteLambdaFieldRemapper(parentRemapper: FieldRemapper): Boolean =
        parentRemapper !is RegeneratedLambdaFieldRemapper && parentRemapper !is InlinedLambdaRemapper
}
