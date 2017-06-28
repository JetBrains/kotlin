/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.coroutines.COROUTINE_IMPL_ASM_TYPE
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.Companion.NO_ORIGIN
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.*

class AnonymousObjectTransformer(
        transformationInfo: AnonymousObjectTransformationInfo,
        private val inliningContext: InliningContext,
        private val isSameModule: Boolean
) : ObjectTransformer<AnonymousObjectTransformationInfo>(transformationInfo, inliningContext.state) {

    private val oldObjectType = Type.getObjectType(transformationInfo.oldClassName)

    private val fieldNames = hashMapOf<String, MutableList<String>>()

    private var constructor: MethodNode? = null
    private var sourceInfo: String? = null
    private var debugInfo: String? = null
    private lateinit var sourceMapper: SourceMapper

    override fun doTransform(parentRemapper: FieldRemapper): InlineResult {
        val innerClassNodes = ArrayList<InnerClassNode>()
        val classBuilder = createRemappingClassBuilderViaFactory(inliningContext)
        val methodsToTransform = ArrayList<MethodNode>()

        createClassReader().accept(object : ClassVisitor(API, classBuilder.visitor) {
            override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<String>) {
                classBuilder.defineClass(null, version, access, name, signature, superName, interfaces)
                if (COROUTINE_IMPL_ASM_TYPE.internalName == superName) {
                    inliningContext.isContinuation = true
                }
            }

            override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
                innerClassNodes.add(InnerClassNode(name, outerName, innerName, access))
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
                }
                else {
                    methodsToTransform.add(node)
                }
                return node
            }

            override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
                addUniqueField(name)
                return if (isCapturedFieldName(name)) {
                    null
                }
                else {
                    classBuilder.newField(JvmDeclarationOrigin.NO_ORIGIN, access, name, desc, signature, value)
                }
            }

            override fun visitSource(source: String, debug: String?) {
                sourceInfo = source
                debugInfo = debug
            }

            override fun visitEnd() {}
        }, ClassReader.SKIP_FRAMES)

        if (!inliningContext.isInliningLambda) {
            sourceMapper = if (debugInfo != null && !debugInfo!!.isEmpty()) {
                SourceMapper.createFromSmap(SMAPParser.parse(debugInfo!!))
            }
            else {
                //seems we can't do any clever mapping cause we don't know any about original class name
                IdenticalSourceMapper
            }
            if (sourceInfo != null && !GENERATE_SMAP) {
                classBuilder.visitSource(sourceInfo!!, debugInfo)
            }
        }
        else {
            if (sourceInfo != null) {
                classBuilder.visitSource(sourceInfo!!, debugInfo)
            }
            sourceMapper = IdenticalSourceMapper
        }

        val allCapturedParamBuilder = ParametersBuilder.newBuilder()
        val constructorParamBuilder = ParametersBuilder.newBuilder()
        val additionalFakeParams = extractParametersMappingAndPatchConstructor(
                constructor!!, allCapturedParamBuilder, constructorParamBuilder,transformationInfo, parentRemapper
        )
        val deferringMethods = ArrayList<DeferredMethodVisitor>()

        generateConstructorAndFields(classBuilder, allCapturedParamBuilder, constructorParamBuilder, parentRemapper, additionalFakeParams)

        for (next in methodsToTransform) {
            val deferringVisitor = newMethod(classBuilder, next)
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
            removeFinallyMarkers(method.intermediate)
            method.visitEnd()
        }

        SourceMapper.flushToClassBuilder(sourceMapper, classBuilder)

        val visitor = classBuilder.visitor
        innerClassNodes.forEach {
            node ->
            visitor.visitInnerClass(node.name, node.outerName, node.innerName, node.access)
        }

        writeOuterInfo(visitor)

        classBuilder.done()

        return transformationResult
    }

    private fun writeOuterInfo(visitor: ClassVisitor) {
        val info = inliningContext.callSiteInfo
        visitor.visitOuterClass(info.ownerClassName, info.functionName, info.functionDesc)
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
        val parameters = if (isConstructor) capturedBuilder.buildParameters() else getMethodParametersWithCaptured(capturedBuilder, sourceNode)

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
                sourceMapper,
                InlineCallSiteInfo(
                        transformationInfo.oldClassName,
                        sourceNode.name,
                        if (isConstructor) transformationInfo.newConstructorDescriptor else sourceNode.desc
                ), null
        )

        val result = inliner.doInline(deferringVisitor, LocalVarRemapper(parameters, 0), false, LabelOwner.NOT_APPLICABLE)
        result.reifiedTypeParametersUsages.mergeAll(typeParametersToReify)
        deferringVisitor.visitMaxs(-1, -1)
        return result
    }

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
                    descTypes.add(info.getType())
                }
                size += info.getType().size
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

        var paramIndex = 0
        val capturedFieldInitializer = InstructionAdapter(constructorVisitor)
        for (i in fieldInfoWithSkipped.indices) {
            val fieldInfo = fieldInfoWithSkipped[i]
            if (!newFieldsWithSkipped[i].skip) {
                AsmUtil.genAssignInstanceFieldFromParam(fieldInfo, capturedIndexes[paramIndex], capturedFieldInitializer)
            }
            paramIndex++
        }

        //then transform constructor
        //HACK: in inlinining into constructor we access original captured fields with field access not local var
        //but this fields added to general params (this assumes local var access) not captured one,
        //so we need to add them to captured params
        for (info in constructorAdditionalFakeParams) {
            val fake = constructorInlineBuilder.addCapturedParamCopy(info)

            if (fake.lambda != null) {
                //set remap value to skip this fake (captured with lambda already skipped)
                val composed = StackValue.field(
                        fake.getType(),
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
        val oldStartLabel = if (first is LabelNode) first.label else null
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
        AsmUtil.genClosureFields(
                toNameTypePair(filterSkipped(newFieldsWithSkipped)), classBuilder
        )
    }

    private fun getMethodParametersWithCaptured(capturedBuilder: ParametersBuilder, sourceNode: MethodNode): Parameters {
        val builder = ParametersBuilder.initializeBuilderFrom(oldObjectType, sourceNode.desc)
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
        val indexToLambda = transformationInfo.lambdasToInline
        val capturedParams = HashSet<Int>()

        //load captured parameters and patch instruction list
        //  NB: there is also could be object fields
        val toDelete = arrayListOf<AbstractInsnNode>()
        constructor.findCapturedFieldAssignmentInstructions().
                forEach { fieldNode ->
                    val fieldName = fieldNode.name
                    val parameterAload = fieldNode.previous as VarInsnNode
                    val varIndex = parameterAload.`var`
                    val lambdaInfo = indexToLambda[varIndex]
                    val newFieldName = if (isThis0(fieldName) && shouldRenameThis0(parentFieldRemapper, indexToLambda.values))
                        getNewFieldName(fieldName, true)
                    else
                        fieldName
                    val info = capturedParamBuilder.addCapturedParam(
                            Type.getObjectType(transformationInfo.oldClassName), fieldName, newFieldName,
                            Type.getType(fieldNode.desc), lambdaInfo != null, null
                    )
                    if (lambdaInfo != null) {
                        info.lambda = lambdaInfo
                        capturedLambdas.add(lambdaInfo)
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
            val info = indexToLambda[constructorParamBuilder.nextParameterOffset]
            val parameterInfo = constructorParamBuilder.addNextParameter(type, info != null)
            parameterInfo.lambda = info
            if (capturedParams.contains(parameterInfo.index)) {
                parameterInfo.isCaptured = true
            }
            else {
                //otherwise it's super constructor parameter
            }
        }

        //For all inlined lambdas add their captured parameters
        //TODO: some of such parameters could be skipped - we should perform additional analysis
        val capturedLambdasToInline = HashMap<String, LambdaInfo>() //captured var of inlined parameter
        val allRecapturedParameters = ArrayList<CapturedParamDesc>()
        val addCapturedNotAddOuter = parentFieldRemapper.isRoot || parentFieldRemapper is InlinedLambdaRemapper && parentFieldRemapper.parent!!.isRoot
        val alreadyAdded = HashMap<String, CapturedParamInfo>()
        for (info in capturedLambdas) {
            if (addCapturedNotAddOuter) {
                for (desc in info.capturedVars) {
                    val key = desc.fieldName + "$$$" + desc.type.className
                    val alreadyAddedParam = alreadyAdded[key]

                    val recapturedParamInfo = capturedParamBuilder.addCapturedParam(
                            desc,
                            alreadyAddedParam?.newFieldName ?: getNewFieldName(desc.fieldName, false),
                            alreadyAddedParam != null
                    )
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
            capturedLambdasToInline.put(info.lambdaClassType.internalName, info)
        }

        if (parentFieldRemapper is InlinedLambdaRemapper && !capturedLambdas.isEmpty() && !addCapturedNotAddOuter) {
            //lambda with non InlinedLambdaRemapper already have outer
            val parent = parentFieldRemapper.parent as? RegeneratedLambdaFieldRemapper ?:
                         throw AssertionError("Expecting RegeneratedLambdaFieldRemapper, but ${parentFieldRemapper.parent}")
            val ownerType = Type.getObjectType(parent.originalLambdaInternalName)
            val desc = CapturedParamDesc(ownerType, THIS, ownerType)
            val recapturedParamInfo = capturedParamBuilder.addCapturedParam(desc, THIS_0/*outer lambda/object*/, false)
            val composed = StackValue.LOCAL_0
            recapturedParamInfo.remapValue = composed
            allRecapturedParameters.add(desc)

            constructorParamBuilder.addCapturedParam(recapturedParamInfo, recapturedParamInfo.newFieldName).remapValue = composed
        }

        transformationInfo.allRecapturedParameters = allRecapturedParameters
        transformationInfo.capturedLambdasToInline = capturedLambdasToInline

        return constructorAdditionalFakeParams
    }

    private fun shouldRenameThis0(parentFieldRemapper: FieldRemapper, values: Collection<LambdaInfo>): Boolean {
        if (isFirstDeclSiteLambdaFieldRemapper(parentFieldRemapper)) {
            for (value in values) {
                for (desc in value.capturedVars) {
                    if (isThis0(desc.fieldName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getNewFieldName(oldName: String, originalField: Boolean): String {
        if (THIS_0 == oldName) {
            return if (!originalField) {
                oldName
            }
            else {
                //rename original 'this$0' in declaration site lambda (inside inline function) to use this$0 only for outer lambda/object access on call site
                addUniqueField(oldName + INLINE_FUN_THIS_0_SUFFIX)
            }
        }
        return addUniqueField(oldName + INLINE_TRANSFORMATION_SUFFIX)
    }

    private fun addUniqueField(name: String): String {
        val existNames = fieldNames.getOrPut(name) { LinkedList<String>() }
        val suffix = if (existNames.isEmpty()) "" else "$" + existNames.size
        val newName = name + suffix
        existNames.add(newName)
        return newName
    }

    private fun isFirstDeclSiteLambdaFieldRemapper(parentRemapper: FieldRemapper): Boolean {
        return parentRemapper !is RegeneratedLambdaFieldRemapper && parentRemapper !is InlinedLambdaRemapper
    }
}
