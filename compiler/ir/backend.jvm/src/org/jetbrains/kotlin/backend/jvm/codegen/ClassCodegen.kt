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
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
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

abstract class ClassCodegen protected constructor(
    val irClass: IrClass,
    val context: JvmBackendContext,
    private val parentFunction: IrFunction?,
) : InnerClassConsumer {
    protected val parentClassCodegen = (parentFunction?.parentAsClass ?: irClass.parent as? IrClass)?.let { getOrCreate(it, context) }
    private val withinInline: Boolean = parentClassCodegen?.withinInline == true || parentFunction?.isInline == true

    protected val state get() = context.state
    protected val typeMapper get() = context.typeMapper

    val type: Type = typeMapper.mapClass(irClass)

    val reifiedTypeParametersUsages = ReifiedTypeParametersUsages()

    private val jvmSignatureClashDetector = JvmSignatureClashDetector(irClass, type, context)

    private val classOrigin = run {
        // The descriptor associated with an IrClass is never modified in lowerings, so it
        // doesn't reflect the state of the lowered class. To make the diagnostics work we
        // pass in a wrapped descriptor instead, except for lambdas where we use the descriptor
        // of the original function.
        // TODO: Migrate class builders away from descriptors
        val descriptor = irClass.toIrBasedDescriptor()
        val psiElement = context.psiSourceManager.findPsiElement(irClass)
        when (irClass.origin) {
            IrDeclarationOrigin.FILE_CLASS ->
                JvmDeclarationOrigin(JvmDeclarationOriginKind.PACKAGE_PART, psiElement, descriptor)
            JvmLoweredDeclarationOrigin.LAMBDA_IMPL, JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL ->
                OtherOrigin(
                    psiElement,
                    irClass.attributeOwnerId.safeAs<IrFunctionReference>()?.symbol?.owner?.toIrBasedDescriptor() ?: descriptor
                )
            else ->
                OtherOrigin(psiElement, descriptor)
        }
    }

    protected val visitor = state.factory.newVisitor(classOrigin, type, irClass.fileParent.loadSourceFilesInfo()).apply {
        val signature = getSignature(irClass, type, irClass.getSuperClassInfo(typeMapper), typeMapper)
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

    private val innerClasses = linkedSetOf<IrClass>()

    private var regeneratedObjectNameGenerators = mutableMapOf<String, NameGenerator>()

    fun getRegeneratedObjectNameGenerator(function: IrFunction): NameGenerator {
        val name = if (function.name.isSpecial) "special" else function.name.asString()
        return regeneratedObjectNameGenerators.getOrPut(name) {
            NameGenerator("${type.internalName}\$$name\$\$inlined")
        }
    }

    private var hasAssertField = irClass.hasAssertionsDisabledField(context)
    private var classInitializer = irClass.functions.singleOrNull { it.name.asString() == "<clinit>" }
    private var generatingClInit = false
    private var generated = false

    fun generate(parentDelegatedPropertyTracker: DelegatedPropertyOptimizer? = null): ReifiedTypeParametersUsages {
        // TODO: reject repeated generate() calls; currently, these can happen for objects in finally
        //       blocks since they are `accept`ed once per each CFG edge out of the try-finally.
        if (generated) return reifiedTypeParametersUsages
        generated = true

        // We remove unused cached KProperties.
        val classDelegatedPropertiesArray = irClass.fields.singleOrNull {
            it.origin == JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE
        }
        val delegatedPropertyTracker =
            if (classDelegatedPropertiesArray != null) DelegatedPropertyOptimizer() else parentDelegatedPropertyTracker

        val smap = context.getSourceMapper(irClass)
        for (declaration in irClass.declarations) {
            when (declaration) {
                is IrClass, classInitializer, classDelegatedPropertiesArray -> Unit // see below
                is IrField -> generateField(declaration)
                is IrFunction -> generateMethod(declaration, smap, delegatedPropertyTracker)
                else -> throw AssertionError("unexpected class member $declaration at codegen")
            }
        }

        // Generate nested classes at the end, to ensure that when the companion's metadata is serialized
        // everything moved to the outer class has already been recorded in `globalSerializationBindings`.
        for (declaration in irClass.declarations) {
            if (declaration is IrClass) {
                getOrCreate(declaration, context).generate(delegatedPropertyTracker)
            }
        }

        // Delay generation of <clinit> until the end because inline function calls
        // might need to generate the `$assertionsDisabled` field initializer.
        classInitializer?.let {
            generatingClInit = true
            generateMethod(it, smap, delegatedPropertyTracker)
            if (classDelegatedPropertiesArray != null && delegatedPropertyTracker?.needsDelegatedProperties == true) {
                generateField(classDelegatedPropertiesArray)
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

        generateInnerClasses()

        return reifiedTypeParametersUsages
    }

    fun generateAssertFieldIfNeeded(): IrExpression? {
        if (hasAssertField)
            return null
        hasAssertField = true
        val topLevelClass = generateSequence(this) { it.parentClassCodegen }.last().irClass
        val field = irClass.buildAssertionsDisabledField(context, topLevelClass)
        generateField(field)
        // Normally, `InitializersLowering` would move the initializer to <clinit>, but
        // it's obviously too late for that.
        val init = IrSetFieldImpl(
            field.startOffset, field.endOffset, field.symbol, null,
            field.initializer!!.expression, context.irBuiltIns.unitType
        )
        if (classInitializer == null) {
            classInitializer = context.irFactory.buildFun {
                name = Name.special("<clinit>")
                returnType = context.irBuiltIns.unitType
            }.apply {
                parent = irClass
                body = IrBlockBodyImpl(startOffset, endOffset)
            }
            // Do not add it to `irClass.declarations` to avoid a concurrent modification error.
        } else if (generatingClInit) {
            // Not only `classInitializer` is non-null, we're in fact generating it right now.
            // Attempting to do `body.statements.add` will cause a concurrent modification error,
            // so the currently active ExpressionCodegen needs to be asked to generate this
            // initializer directly.
            return init
        }
        (classInitializer!!.body as IrBlockBody).statements.add(0, init)
        return null
    }

    protected abstract fun generateKotlinMetadataAnnotation()

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
            context.classCodegens.getOrPut(irClass) {
                context.createCodegen(irClass, context, parentFunction) ?: DescriptorBasedClassCodegen(irClass, context, parentFunction)
            }.also {
                assert(parentFunction == null || it.parentFunction == parentFunction) {
                    "inconsistent parent function for ${irClass.render()}:\n" +
                            "New: ${parentFunction!!.render()}\n" +
                            "Old: ${it.parentFunction?.render()}"
                }
            }

        private fun JvmClassSignature.hasInvalidName() =
            name.splitToSequence('/').any { identifier -> identifier.any { it in JvmSimpleNameBacktickChecker.INVALID_CHARS } }
    }

    protected abstract fun bindFieldMetadata(field: IrField, fieldType: Type, fieldName: String)

    private fun generateField(field: IrField) {
        val fieldType = typeMapper.mapType(field)
        val fieldSignature =
            if (field.origin == IrDeclarationOrigin.PROPERTY_DELEGATE) null
            else context.methodSignatureMapper.mapFieldSignature(field)
        val fieldName = field.name.asString()
        val flags = field.flags
        val fv = visitor.newField(
            field.OtherOrigin, flags, fieldName, fieldType.descriptor,
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

        bindFieldMetadata(field, fieldType, fieldName)
    }

    private val generatedInlineMethods = mutableMapOf<IrFunction, SMAPAndMethodNode>()

    fun generateMethodNode(method: IrFunction, delegatedPropertyOptimizer: DelegatedPropertyOptimizer?): SMAPAndMethodNode {
        if (!method.isInline && !method.isSuspend) {
            // Inline methods can be used multiple times by `IrSourceCompilerForInline`, suspend methods
            // could be used twice if they capture crossinline lambdas, and everything else is only
            // generated by `generateMethod` below so does not need caching.
            return FunctionCodegen(method, this).generate(delegatedPropertyOptimizer)
        }
        val (node, smap) = generatedInlineMethods.getOrPut(method) { FunctionCodegen(method, this).generate(delegatedPropertyOptimizer) }
        val copy = with(node) { MethodNode(Opcodes.API_VERSION, access, name, desc, signature, exceptions.toTypedArray()) }
        node.instructions.resetLabels()
        node.accept(copy)
        return SMAPAndMethodNode(copy, smap)
    }

    protected abstract fun bindMethodMetadata(method: IrFunction, signature: Method)

    private fun generateMethod(method: IrFunction, classSMAP: SourceMapper, delegatedPropertyOptimizer: DelegatedPropertyOptimizer?) {
        if (method.isFakeOverride) {
            jvmSignatureClashDetector.trackFakeOverrideMethod(method)
            return
        }

        val (node, smap) = generateMethodNode(method, delegatedPropertyOptimizer)
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
        val mv = with(node) { visitor.newMethod(method.OtherOrigin, access, name, desc, signature, exceptions.toTypedArray()) }
        val smapCopier = SourceMapCopier(classSMAP, smap)
        val smapCopyingVisitor = object : MethodVisitor(Opcodes.API_VERSION, mv) {
            override fun visitLineNumber(line: Int, start: Label) =
                super.visitLineNumber(smapCopier.mapLineNumber(line), start)
        }
        if (method.hasContinuation() || method.isInvokeSuspendOfLambda()) {
            // Generate a state machine within this method. The continuation class for it should be generated
            // lazily so that if tail call optimization kicks in, the unused class will not be written to the output.
            val continuationClassCodegen = lazy { getOrCreate(method.continuationClass()!!, context, method) }
            node.acceptWithStateMachine(method, this, smapCopyingVisitor) {
                if (method.isSuspend) continuationClassCodegen.value.visitor else visitor
            }
            if (continuationClassCodegen.isInitialized() || method.alwaysNeedsContinuation()) {
                continuationClassCodegen.value.generate(delegatedPropertyOptimizer)
            }
        } else {
            node.accept(smapCopyingVisitor)
        }
        jvmSignatureClashDetector.trackMethod(method, RawSignature(node.name, node.desc, MemberKind.METHOD))

        val signature = Method(node.name, node.desc)
        bindMethodMetadata(method, signature)
    }

    private fun generateInnerAndOuterClasses() {
        // JVMS7 (4.7.6): a nested class or interface member will have InnerClasses information
        // for each enclosing class and for each immediate member
        parentClassCodegen?.let { writeInnerClass(irClass, typeMapper, context, it.visitor) }
        for (codegen in generateSequence(this) { it.parentClassCodegen }.takeWhile { it.parentClassCodegen != null }) {
            writeInnerClass(codegen.irClass, typeMapper, context, visitor)
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

    private fun generateInnerClasses() {
        for (klass in innerClasses) {
            writeInnerClass(klass, typeMapper, context, visitor)
        }
    }
}

private val IrClass.flags: Int
    get() = origin.flags or getVisibilityAccessFlagForClass() or deprecationFlags or when {
        isAnnotationClass -> Opcodes.ACC_ANNOTATION or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
        isInterface -> Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
        isEnumClass -> Opcodes.ACC_ENUM or Opcodes.ACC_SUPER or modality.flags
        else -> Opcodes.ACC_SUPER or modality.flags
    }

private val IrField.flags: Int
    get() = origin.flags or visibility.flags or
            this.specialDeprecationFlag or (correspondingPropertySymbol?.owner?.deprecationFlags ?: 0) or
            (if (annotations.hasAnnotation(KOTLIN_DEPRECATED)) Opcodes.ACC_DEPRECATED else 0) or
            (if (isFinal) Opcodes.ACC_FINAL else 0) or
            (if (isStatic) Opcodes.ACC_STATIC else 0) or
            (if (hasAnnotation(VOLATILE_ANNOTATION_FQ_NAME)) Opcodes.ACC_VOLATILE else 0) or
            (if (hasAnnotation(TRANSIENT_ANNOTATION_FQ_NAME)) Opcodes.ACC_TRANSIENT else 0) or
            (if (hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)) Opcodes.ACC_SYNTHETIC else 0)

private val IrField.specialDeprecationFlag: Int
    get() = if (shouldHaveSpecialDeprecationFlag()) Opcodes.ACC_DEPRECATED else 0

private val JAVA_LANG_DEPRECATED = FqName("java.lang.Deprecated")
private val KOTLIN_DEPRECATED = FqName("kotlin.Deprecated")

fun IrField.shouldHaveSpecialDeprecationFlag(): Boolean {
    return origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE &&
            annotations.hasAnnotation(JAVA_LANG_DEPRECATED)
}

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
    get() = AsmUtil.getVisibilityAccessFlag(this) ?: throw AssertionError("Unsupported visibility $this")

internal val IrDeclaration.OtherOrigin: JvmDeclarationOrigin
    get() {
        val klass = (this as? IrClass) ?: parentAsClass
        return OtherOrigin(
            // For declarations inside lambdas, produce a descriptor which refers back to the original function.
            // This is needed for plugins which check for lambdas inside of inline functions using the descriptor
            // contained in JvmDeclarationOrigin. This matches the behavior of the JVM backend.
            if (klass.origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL || klass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA) {
                klass.attributeOwnerId.safeAs<IrFunctionReference>()?.symbol?.owner?.toIrBasedDescriptor() ?: toIrBasedDescriptor()
            } else {
                toIrBasedDescriptor()
            }
        )
    }

private fun IrClass.getSuperClassInfo(typeMapper: IrTypeMapper): IrSuperClassInfo {
    if (isInterface) {
        return IrSuperClassInfo(AsmTypes.OBJECT_TYPE, null)
    }

    for (superType in superTypes) {
        val superClass = superType.safeAs<IrSimpleType>()?.classifier?.safeAs<IrClassSymbol>()?.owner
        if (superClass != null && !superClass.isJvmInterface) {
            return IrSuperClassInfo(typeMapper.mapClass(superClass), superType)
        }
    }

    return IrSuperClassInfo(AsmTypes.OBJECT_TYPE, null)
}
