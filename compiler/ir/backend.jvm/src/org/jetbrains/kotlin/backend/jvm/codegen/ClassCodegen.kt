/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.lower.buildAssertionsDisabledField
import org.jetbrains.kotlin.backend.jvm.lower.hasAssertionsDisabledField
import org.jetbrains.kotlin.codegen.DescriptorAsmUtil
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.writeKotlinMetadata
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_SYNTHETIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.TRANSIENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.VOLATILE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.checkers.JvmSimpleNameBacktickChecker
import org.jetbrains.kotlin.resolve.jvm.diagnostics.*
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmClassSignature
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.io.File

interface MetadataSerializer {
    fun serialize(metadata: MetadataSource): Pair<MessageLite, JvmStringTable>?
    fun bindMethodMetadata(metadata: MetadataSource.Property, signature: Method)
    fun bindMethodMetadata(metadata: MetadataSource.Function, signature: Method)
    fun bindFieldMetadata(metadata: MetadataSource.Property, signature: Pair<Type, String>)
}

class ClassCodegen private constructor(
    val irClass: IrClass,
    val context: JvmBackendContext,
    private val parentFunction: IrFunction?,
) : InnerClassConsumer {
    private val parentClassCodegen = (parentFunction?.parentAsClass ?: irClass.parent as? IrClass)?.let { getOrCreate(it, context) }
    private val withinInline: Boolean = parentClassCodegen?.withinInline == true || parentFunction?.isInline == true

    private val state get() = context.state
    private val typeMapper get() = context.typeMapper

    val type: Type = typeMapper.mapClass(irClass)

    val reifiedTypeParametersUsages = ReifiedTypeParametersUsages()

    private val jvmSignatureClashDetector = JvmSignatureClashDetector(irClass, type, context)

    private val classOrigin = irClass.descriptorOrigin

    private val visitor = state.factory.newVisitor(classOrigin, type, irClass.fileParent.loadSourceFilesInfo()).apply {
        val signature = typeMapper.mapClassSignature(irClass, type)
        // Ensure that the backend only produces class names that would be valid in the frontend for JVM.
        if (context.state.classBuilderMode.generateBodies && signature.hasInvalidName()) {
            throw IllegalStateException("Generating class with invalid name '${type.className}': ${irClass.dump()}")
        }
        defineClass(
            irClass.psiElement,
            state.classFileVersion,
            irClass.flags,
            signature.name,
            signature.javaGenericSignature,
            signature.superclassName,
            signature.interfaces.toTypedArray()
        )
    }

    // TODO: the order of entries in this set depends on the order in which methods are generated; this means it is unstable
    //       under incremental compilation, as calls to `inline fun`s declared in this class cause them to be generated out of order.
    private val innerClasses = linkedSetOf<IrClass>()

    // TODO: the names produced by generators in this map depend on the order in which methods are generated; see above.
    private val regeneratedObjectNameGenerators = mutableMapOf<String, NameGenerator>()

    fun getRegeneratedObjectNameGenerator(function: IrFunction): NameGenerator {
        val name = if (function.name.isSpecial) "special" else function.name.asString()
        return regeneratedObjectNameGenerators.getOrPut(name) {
            NameGenerator("${type.internalName}\$$name\$\$inlined")
        }
    }

    private var generated = false

    fun generate() {
        // TODO: reject repeated generate() calls; currently, these can happen for objects in finally
        //       blocks since they are `accept`ed once per each CFG edge out of the try-finally.
        if (generated) return
        generated = true

        // We remove reads of `$$delegatedProperties` (and the field itself) if they are not in fact used for anything.
        val delegatedProperties = irClass.fields.singleOrNull { it.origin == JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE }
        val delegatedPropertyOptimizer = if (delegatedProperties != null) DelegatedPropertyOptimizer() else null
        // Generating a method node may cause the addition of a field with an initializer if an inline function
        // call uses `assert` and the JVM assertions mode is enabled. To avoid concurrent modification errors,
        // there is a very specific generation order.
        val smap = context.getSourceMapper(irClass)
        // 1. Any method other than `<clinit>` can add a field and a `<clinit>` statement:
        for (method in irClass.declarations.filterIsInstance<IrFunction>()) {
            if (method.name.asString() != "<clinit>") {
                generateMethod(method, smap, delegatedPropertyOptimizer)
            }
        }
        // 2. `<clinit>` itself can add a field, but the statement is generated via the `return init` hack:
        irClass.functions.find { it.name.asString() == "<clinit>" }?.let { generateMethod(it, smap, delegatedPropertyOptimizer) }
        // 3. Now we have all the fields (`$$delegatedProperties` might be redundant if all reads were optimized out):
        for (field in irClass.fields) {
            if (field !== delegatedProperties || delegatedPropertyOptimizer?.needsDelegatedProperties == true) {
                generateField(field)
            }
        }
        // 4. Generate nested classes at the end, to ensure that when the companion's metadata is serialized
        //    everything moved to the outer class has already been recorded in `globalSerializationBindings`.
        for (declaration in irClass.declarations) {
            if (declaration is IrClass) {
                getOrCreate(declaration, context).generate()
            }
        }

        object : AnnotationCodegen(this@ClassCodegen, context) {
            override fun visitAnnotation(descr: String?, visible: Boolean): AnnotationVisitor {
                return visitor.visitor.visitAnnotation(descr, visible)
            }
        }.genAnnotations(irClass, null, null)
        generateKotlinMetadataAnnotation()

        generateInnerAndOuterClasses()

        if (withinInline || !smap.isTrivial) {
            visitor.visitSMAP(smap, !context.state.languageVersionSettings.supportsFeature(LanguageFeature.CorrectSourceMappingSyntax))
        } else {
            smap.sourceInfo!!.sourceFileName?.let {
                visitor.visitSource(it, null)
            }
        }

        visitor.done()
        jvmSignatureClashDetector.reportErrors(classOrigin)
    }

    fun generateAssertFieldIfNeeded(generatingClInit: Boolean): IrExpression? {
        if (irClass.hasAssertionsDisabledField(context))
            return null
        val topLevelClass = generateSequence(this) { it.parentClassCodegen }.last().irClass
        val field = irClass.buildAssertionsDisabledField(context, topLevelClass)
        generateField(field)
        // Normally, `InitializersLowering` would move the initializer to <clinit>, but
        // it's obviously too late for that.
        val init = IrSetFieldImpl(
            field.startOffset, field.endOffset, field.symbol, null,
            field.initializer!!.expression, context.irBuiltIns.unitType
        )
        if (generatingClInit) {
            // Too late to modify the IR; have to ask the currently active `ExpressionCodegen`
            // to generate this statement directly.
            return init
        }
        val classInitializer = irClass.functions.singleOrNull { it.name.asString() == "<clinit>" } ?: irClass.addFunction {
            name = Name.special("<clinit>")
            returnType = context.irBuiltIns.unitType
        }.apply {
            body = IrBlockBodyImpl(startOffset, endOffset)
        }
        (classInitializer.body as IrBlockBody).statements.add(0, init)
        return null
    }

    private val metadataSerializer: MetadataSerializer =
        context.serializerFactory(context, irClass, type, visitor.serializationBindings, parentClassCodegen?.metadataSerializer)

    private fun generateKotlinMetadataAnnotation() {
        // TODO: if `-Xmultifile-parts-inherit` is enabled, write the corresponding flag for parts and facades to [Metadata.extraInt].
        var extraFlags = JvmAnnotationNames.METADATA_JVM_IR_FLAG
        if (state.isIrWithStableAbi) {
            extraFlags += JvmAnnotationNames.METADATA_JVM_IR_STABLE_ABI_FLAG
        }

        val facadeClassName = context.multifileFacadeForPart[irClass.attributeOwnerId]
        val metadata = irClass.metadata
        val entry = irClass.fileParent.fileEntry
        val kind = when {
            facadeClassName != null -> KotlinClassHeader.Kind.MULTIFILE_CLASS_PART
            metadata is MetadataSource.Class -> KotlinClassHeader.Kind.CLASS
            metadata is MetadataSource.File -> KotlinClassHeader.Kind.FILE_FACADE
            metadata is MetadataSource.Function -> KotlinClassHeader.Kind.SYNTHETIC_CLASS
            entry is MultifileFacadeFileEntry -> KotlinClassHeader.Kind.MULTIFILE_CLASS
            else -> KotlinClassHeader.Kind.SYNTHETIC_CLASS
        }
        writeKotlinMetadata(visitor, state, kind, extraFlags) {
            if (metadata != null) {
                metadataSerializer.serialize(metadata)?.let { (proto, stringTable) ->
                    DescriptorAsmUtil.writeAnnotationData(it, proto, stringTable)
                }
            }

            if (entry is MultifileFacadeFileEntry) {
                val arv = it.visitArray(JvmAnnotationNames.METADATA_DATA_FIELD_NAME)
                for (partFile in entry.partFiles) {
                    val fileClass = partFile.declarations.singleOrNull { it.isFileClass } as IrClass?
                    if (fileClass != null) arv.visit(null, typeMapper.mapClass(fileClass).internalName)
                }
                arv.visitEnd()
            }

            if (facadeClassName != null) {
                it.visit(JvmAnnotationNames.METADATA_MULTIFILE_CLASS_NAME_FIELD_NAME, facadeClassName.internalName)
            }

            if (irClass in context.classNameOverride) {
                val isFileClass = kind == KotlinClassHeader.Kind.MULTIFILE_CLASS ||
                        kind == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART ||
                        kind == KotlinClassHeader.Kind.FILE_FACADE
                assert(isFileClass) { "JvmPackageName is not supported for classes: ${irClass.render()}" }
                it.visit(JvmAnnotationNames.METADATA_PACKAGE_NAME_FIELD_NAME, irClass.fqNameWhenAvailable!!.parent().asString())
            }
        }
    }

    private fun IrFile.loadSourceFilesInfo(): List<File> {
        val entry = fileEntry
        if (entry is MultifileFacadeFileEntry) {
            return entry.partFiles.flatMap { it.loadSourceFilesInfo() }
        }
        return listOfNotNull(context.psiSourceManager.getFileEntry(this)?.let { File(it.name) })
    }

    companion object {
        fun getOrCreate(
            irClass: IrClass,
            context: JvmBackendContext,
            // The `parentFunction` is only set for classes nested inside of functions. This is usually safe, since there is no
            // way to refer to (inline) members of such a class from outside of the function unless the function in question is
            // itself declared as inline. In that case, the function will be compiled before we can refer to the nested class.
            //
            // The one exception to this rule are anonymous objects defined as members of a class. These are nested inside of the
            // class initializer, but can be referred to from anywhere within the scope of the class. That's why we have to ensure
            // that all references to classes inside of <clinit> have a non-null `parentFunction`.
            parentFunction: IrFunction? = irClass.parent.safeAs<IrFunction>()?.takeIf {
                it.origin == JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER
            },
        ): ClassCodegen =
            context.classCodegens.getOrPut(irClass) { ClassCodegen(irClass, context, parentFunction) }.also {
                assert(parentFunction == null || it.parentFunction == parentFunction) {
                    "inconsistent parent function for ${irClass.render()}:\n" +
                            "New: ${parentFunction!!.render()}\n" +
                            "Old: ${it.parentFunction?.render()}"
                }
            }

        private fun JvmClassSignature.hasInvalidName() =
            name.splitToSequence('/').any { identifier -> identifier.any { it in JvmSimpleNameBacktickChecker.INVALID_CHARS } }
    }

    private fun generateField(field: IrField) {
        val fieldType = typeMapper.mapType(field)
        val fieldSignature =
            if (field.origin == IrDeclarationOrigin.PROPERTY_DELEGATE) null
            else context.methodSignatureMapper.mapFieldSignature(field)
        val fieldName = field.name.asString()
        val flags = field.computeFieldFlags(context, state.languageVersionSettings)
        val fv = visitor.newField(
            field.descriptorOrigin, flags, fieldName, fieldType.descriptor,
            fieldSignature, (field.initializer?.expression as? IrConst<*>)?.value
        )

        jvmSignatureClashDetector.trackField(field, RawSignature(fieldName, fieldType.descriptor, MemberKind.FIELD))

        if (field.origin != JvmLoweredDeclarationOrigin.CONTINUATION_CLASS_RESULT_FIELD) {
            val skipNullabilityAnnotations =
                flags and (Opcodes.ACC_SYNTHETIC or Opcodes.ACC_ENUM) != 0 ||
                        field.origin == JvmLoweredDeclarationOrigin.FIELD_FOR_STATIC_CALLABLE_REFERENCE_INSTANCE
            object : AnnotationCodegen(this@ClassCodegen, context, skipNullabilityAnnotations) {
                override fun visitAnnotation(descr: String?, visible: Boolean): AnnotationVisitor {
                    return fv.visitAnnotation(descr, visible)
                }

                override fun visitTypeAnnotation(descr: String?, path: TypePath?, visible: Boolean): AnnotationVisitor {
                    return fv.visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.FIELD).value, path, descr, visible)
                }
            }.genAnnotations(field, fieldType, field.type)
        }

        (field.metadata as? MetadataSource.Property)?.let {
            metadataSerializer.bindFieldMetadata(it, fieldType to fieldName)
        }
    }

    private val generatedInlineMethods = mutableMapOf<IrFunction, SMAPAndMethodNode>()

    fun generateMethodNode(method: IrFunction): SMAPAndMethodNode {
        if (!method.isInline && !method.isSuspendCapturingCrossinline()) {
            // Inline methods can be used multiple times by `IrSourceCompilerForInline`, suspend methods
            // are used twice (`f` and `f$$forInline`) if they capture crossinline lambdas, and everything
            // else is only generated by `generateMethod` below so does not need caching.
            // TODO: inline lambdas are not marked `isInline`, and are generally used once, but may be needed
            //       multiple times if declared in a `finally` block - should they be cached?
            return FunctionCodegen(method, this).generate()
        }
        val (node, smap) = generatedInlineMethods.getOrPut(method) { FunctionCodegen(method, this).generate() }
        val copy = with(node) { MethodNode(Opcodes.API_VERSION, access, name, desc, signature, exceptions.toTypedArray()) }
        node.instructions.resetLabels()
        node.accept(copy)
        return SMAPAndMethodNode(copy, smap)
    }

    private fun generateMethod(method: IrFunction, classSMAP: SourceMapper, delegatedPropertyOptimizer: DelegatedPropertyOptimizer?) {
        if (method.isFakeOverride) {
            jvmSignatureClashDetector.trackFakeOverrideMethod(method)
            return
        }

        val (node, smap) = generateMethodNode(method)
        if (delegatedPropertyOptimizer != null) {
            delegatedPropertyOptimizer.transform(node)
            if (method.name.asString() == "<clinit>") {
                delegatedPropertyOptimizer.transformClassInitializer(node)
            }
        }
        node.preprocessSuspendMarkers(
            method.origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE || method.isEffectivelyInlineOnly(),
            method.origin == JvmLoweredDeclarationOrigin.FOR_INLINE_STATE_MACHINE_TEMPLATE_CAPTURES_CROSSINLINE
        )
        val mv = with(node) { visitor.newMethod(method.descriptorOrigin, access, name, desc, signature, exceptions.toTypedArray()) }
        val smapCopier = SourceMapCopier(classSMAP, smap)
        val smapCopyingVisitor = object : MethodVisitor(Opcodes.API_VERSION, mv) {
            override fun visitLineNumber(line: Int, start: Label) =
                super.visitLineNumber(smapCopier.mapLineNumber(line), start)
        }
        if (method.hasContinuation()) {
            // Generate a state machine within this method. The continuation class for it should be generated
            // lazily so that if tail call optimization kicks in, the unused class will not be written to the output.
            val continuationClass = method.continuationClass() // null if `SuspendLambda.invokeSuspend` - `this` is continuation itself
            val continuationClassCodegen = lazy { if (continuationClass != null) getOrCreate(continuationClass, context, method) else this }
            node.acceptWithStateMachine(method, this, smapCopyingVisitor) { continuationClassCodegen.value.visitor }
            if (continuationClass != null && (continuationClassCodegen.isInitialized() || method.isSuspendCapturingCrossinline())) {
                continuationClassCodegen.value.generate()
            }
        } else {
            node.accept(smapCopyingVisitor)
        }
        jvmSignatureClashDetector.trackMethod(method, RawSignature(node.name, node.desc, MemberKind.METHOD))

        when (val metadata = method.metadata) {
            is MetadataSource.Property -> {
                assert(method.isSyntheticMethodForProperty) {
                    "MetadataSource.Property on IrFunction should only be used for synthetic \$annotations methods: ${method.render()}"
                }
                metadataSerializer.bindMethodMetadata(metadata, Method(node.name, node.desc))
            }
            is MetadataSource.Function -> metadataSerializer.bindMethodMetadata(metadata, Method(node.name, node.desc))
            null -> Unit
            else -> error("Incorrect metadata source $metadata for:\n${method.dump()}")
        }
    }

    private fun generateInnerAndOuterClasses() {
        // JVMS7 (4.7.6): a nested class or interface member will have InnerClasses information
        // for each enclosing class and for each immediate member
        parentClassCodegen?.innerClasses?.add(irClass)
        for (codegen in generateSequence(this) { it.parentClassCodegen }.takeWhile { it.parentClassCodegen != null }) {
            innerClasses.add(codegen.irClass)
        }

        // JVMS7 (4.7.7): A class must have an EnclosingMethod attribute if and only if
        // it is a local class or an anonymous class.
        //
        // The attribute contains the innermost class that encloses the declaration of
        // the current class. If the current class is immediately enclosed by a method
        // or constructor, the name and type of the function is recorded as well.
        if (parentClassCodegen != null) {
            // In case there's no primary constructor, it's unclear which constructor should be the enclosing one, so we select the first.
            val enclosingFunction = if (irClass.attributeOwnerId in context.isEnclosedInConstructor) {
                val containerClass = parentClassCodegen.irClass
                containerClass.primaryConstructor
                    ?: containerClass.declarations.firstIsInstanceOrNull<IrConstructor>()
                    ?: error("Class in a non-static initializer found, but container has no constructors: ${containerClass.render()}")
            } else parentFunction
            if (enclosingFunction != null || irClass.isAnonymousObject) {
                val method = enclosingFunction?.let(context.methodSignatureMapper::mapAsmMethod)
                visitor.visitOuterClass(parentClassCodegen.type.internalName, method?.name, method?.descriptor)
            }
        }

        for (klass in innerClasses) {
            val innerClass = typeMapper.classInternalName(klass)
            val outerClass =
                if (klass.attributeOwnerId in context.isEnclosedInConstructor) null
                else klass.parent.safeAs<IrClass>()?.let(typeMapper::classInternalName)
            val innerName = klass.name.takeUnless { it.isSpecial }?.asString()
            visitor.visitInnerClass(innerClass, outerClass, innerName, klass.calculateInnerClassAccessFlags(context))
        }
    }

    override fun addInnerClassInfoFromAnnotation(innerClass: IrClass) {
        // It's necessary for proper recovering of classId by plain string JVM descriptor when loading annotations
        // See FileBasedKotlinClass.convertAnnotationVisitor
        generateSequence<IrDeclaration>(innerClass) { it.parent as? IrDeclaration }.takeWhile { !it.isTopLevelDeclaration }.forEach {
            if (it is IrClass) {
                innerClasses.add(it)
            }
        }
    }

    private val IrDeclaration.descriptorOrigin: JvmDeclarationOrigin
        get() {
            val psiElement = context.psiSourceManager.findPsiElement(this)
            // For declarations inside lambdas, produce a descriptor which refers back to the original function.
            // This is needed for plugins which check for lambdas inside of inline functions using the descriptor
            // contained in JvmDeclarationOrigin. This matches the behavior of the JVM backend.
            // TODO: this is really not very useful, as this does nothing for other anonymous objects.
            val isLambda = irClass.origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL ||
                    irClass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA
            val descriptor = if (isLambda)
                irClass.attributeOwnerId.safeAs<IrFunctionReference>()?.symbol?.owner?.toIrBasedDescriptor() ?: toIrBasedDescriptor()
            else
                toIrBasedDescriptor()
            return if (origin == IrDeclarationOrigin.FILE_CLASS)
                JvmDeclarationOrigin(JvmDeclarationOriginKind.PACKAGE_PART, psiElement, descriptor)
            else
                OtherOrigin(psiElement, descriptor)
        }
}

private val IrClass.flags: Int
    get() = origin.flags or getVisibilityAccessFlagForClass() or
            (if (isAnnotatedWithDeprecated) Opcodes.ACC_DEPRECATED else 0) or
            (if (hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)) Opcodes.ACC_SYNTHETIC else 0) or
            when {
                isAnnotationClass -> Opcodes.ACC_ANNOTATION or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
                isInterface -> Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
                isEnumClass -> Opcodes.ACC_ENUM or Opcodes.ACC_SUPER or modality.flags
                else -> Opcodes.ACC_SUPER or modality.flags
            }

private fun IrField.computeFieldFlags(context: JvmBackendContext, languageVersionSettings: LanguageVersionSettings): Int =
    origin.flags or visibility.flags or
            (if (isDeprecatedCallable(context) ||
                correspondingPropertySymbol?.owner?.isDeprecatedCallable(context) == true
            ) Opcodes.ACC_DEPRECATED else 0) or
            (if (isFinal) Opcodes.ACC_FINAL else 0) or
            (if (isStatic) Opcodes.ACC_STATIC else 0) or
            (if (hasAnnotation(VOLATILE_ANNOTATION_FQ_NAME)) Opcodes.ACC_VOLATILE else 0) or
            (if (hasAnnotation(TRANSIENT_ANNOTATION_FQ_NAME)) Opcodes.ACC_TRANSIENT else 0) or
            (if (hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME) ||
                isPrivateCompanionFieldInInterface(languageVersionSettings)
            ) Opcodes.ACC_SYNTHETIC else 0)

private fun IrField.isPrivateCompanionFieldInInterface(languageVersionSettings: LanguageVersionSettings): Boolean =
    origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE &&
            languageVersionSettings.supportsFeature(LanguageFeature.ProperVisibilityForCompanionObjectInstanceField) &&
            parentAsClass.isJvmInterface &&
            DescriptorVisibilities.isPrivate(parentAsClass.companionObject()!!.visibility)

private val IrDeclarationOrigin.flags: Int
    get() = (if (isSynthetic) Opcodes.ACC_SYNTHETIC else 0) or
            (if (this == IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY) Opcodes.ACC_ENUM else 0)

private val Modality.flags: Int
    get() = when (this) {
        Modality.ABSTRACT, Modality.SEALED -> Opcodes.ACC_ABSTRACT
        Modality.FINAL -> Opcodes.ACC_FINAL
        Modality.OPEN -> 0
        else -> throw AssertionError("Unsupported modality $this")
    }

private val DescriptorVisibility.flags: Int
    get() = DescriptorAsmUtil.getVisibilityAccessFlag(this) ?: throw AssertionError("Unsupported visibility $this")
