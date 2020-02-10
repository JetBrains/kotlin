/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.lower.buildAssertionsDisabledField
import org.jetbrains.kotlin.backend.jvm.lower.hasAssertionsDisabledField
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.inline.DefaultSourceMapper
import org.jetbrains.kotlin.codegen.inline.NameGenerator
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeParametersUsages
import org.jetbrains.kotlin.codegen.inline.SourceMapper
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_SYNTHETIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.TRANSIENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.VOLATILE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.checkers.JvmSimpleNameBacktickChecker
import org.jetbrains.kotlin.resolve.jvm.diagnostics.*
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmClassSignature
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.*
import java.io.File

open class ClassCodegen protected constructor(
    internal val irClass: IrClass,
    val context: JvmBackendContext,
    private val parentClassCodegen: ClassCodegen? = null,
    private val parentFunction: IrFunction? = null,
    private val withinInline: Boolean = false
) : InnerClassConsumer {
    private val innerClasses = mutableListOf<IrClass>()

    val state = context.state

    val typeMapper = context.typeMapper
    val methodSignatureMapper = context.methodSignatureMapper

    val type: Type = typeMapper.mapClass(irClass)

    val visitor: ClassBuilder = createClassBuilder()

    val reifiedTypeParametersUsages = ReifiedTypeParametersUsages()

    private val jvmSignatureClashDetector = JvmSignatureClashDetector(irClass, type, context)
    private lateinit var classOrigin: JvmDeclarationOrigin

    open fun createClassBuilder(): ClassBuilder {
        // The descriptor associated with an IrClass is never modified in lowerings, so it
        // doesn't reflect the state of the lowered class. To make the diagnostics work we
        // pass in a wrapped descriptor instead.
        // TODO: Migrate class builders away from descriptors
        val descriptor = WrappedClassDescriptor()
        descriptor.bind(irClass)
        classOrigin = getClassOrigin(descriptor)
        return state.factory.newVisitor(
            classOrigin,
            type,
            irClass.fileParent.loadSourceFilesInfo()
        )
    }

    private fun getClassOrigin(descriptor: ClassDescriptor): JvmDeclarationOrigin {
        val psiElement = context.psiSourceManager.findPsiElement(irClass)
        return when (irClass.origin) {
            IrDeclarationOrigin.FILE_CLASS ->
                JvmDeclarationOrigin(JvmDeclarationOriginKind.PACKAGE_PART, psiElement, descriptor)
            else ->
                OtherOrigin(psiElement, descriptor)
        }
    }

    private var sourceMapper: DefaultSourceMapper? = null

    private val serializerExtension = JvmSerializerExtension(visitor.serializationBindings, state, typeMapper)
    private val serializer: DescriptorSerializer? =
        when (val metadata = irClass.metadata) {
            is MetadataSource.Class -> DescriptorSerializer.create(metadata.descriptor, serializerExtension, parentClassCodegen?.serializer)
            is MetadataSource.File -> DescriptorSerializer.createTopLevel(serializerExtension)
            is MetadataSource.Function -> DescriptorSerializer.createForLambda(serializerExtension)
            else -> null
        }

    fun getRegeneratedObjectNameGenerator(function: IrFunction): NameGenerator {
        val name = if (function.name.isSpecial) Name.identifier("special") else function.name
        return context.regeneratedObjectNameGenerators.getOrPut(irClass to name) {
            NameGenerator("${type.internalName}\$$name\$\$inlined")
        }
    }

    private var classInitializer: IrSimpleFunction? = null
    private var generatingClInit: Boolean = false

    fun generate(): ReifiedTypeParametersUsages {
        if (withinInline) {
            getOrCreateSourceMapper() //initialize default mapping that would be later written in class file
        }
        val superClassInfo = irClass.getSuperClassInfo(typeMapper)
        val signature = getSignature(irClass, type, superClassInfo, typeMapper)

        // Ensure that the backend only produces class names that would be valid in the frontend for JVM.
        if (context.state.classBuilderMode.generateBodies && signature.hasInvalidName()) {
            throw IllegalStateException("Generating class with invalid name '${type.className}': ${irClass.dump()}")
        }

        visitor.defineClass(
            irClass.descriptor.psiElement,
            state.classFileVersion,
            irClass.flags,
            signature.name,
            signature.javaGenericSignature,
            signature.superclassName,
            signature.interfaces.toTypedArray()
        )
        object : AnnotationCodegen(this@ClassCodegen, context) {
            override fun visitAnnotation(descr: String?, visible: Boolean): AnnotationVisitor {
                return visitor.visitor.visitAnnotation(descr, visible)
            }
        }.genAnnotations(
            irClass,
            null,
            null
        )

        val nestedClasses = irClass.declarations.mapNotNull { declaration ->
            if (declaration is IrClass) {
                ClassCodegen(declaration, context, this, withinInline = withinInline)
            } else null
        }

        val fileEntry = context.psiSourceManager.getFileEntry(irClass.fileParent)
        if (fileEntry != null) {
            /* TODO: Temporary workaround: ClassBuilder needs a pathless name. */
            val shortName = File(fileEntry.name).name
            visitor.visitSource(shortName, null)
        }

        // Delay generation of <clinit> until the end because inline function calls
        // might need to generate the `$assertionsDisabled` field initializer.
        classInitializer = irClass.functions.singleOrNull { it.name.asString() == "<clinit>" }
        for (declaration in irClass.declarations) {
            if (declaration != classInitializer)
                generateDeclaration(declaration)
        }
        classInitializer?.let {
            generatingClInit = true
            generateMethod(it)
        }

        // Generate nested classes at the end, to ensure that codegen for companion object will have the necessary JVM signatures in its
        // trace for properties moved to the outer class
        for (codegen in nestedClasses) {
            codegen.generate()
        }

        generateKotlinMetadataAnnotation()

        if (irClass.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS) {
            context.continuationClassBuilders[irClass.attributeOwnerId as IrSimpleFunction] = visitor
        } else {
            done()
        }
        return reifiedTypeParametersUsages
    }

    private var hasAssertField = irClass.hasAssertionsDisabledField(context)

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
            classInitializer = buildFun {
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

    private fun generateKotlinMetadataAnnotation() {
        val localDelegatedProperties = (irClass.attributeOwnerId as? IrClass)?.let(context.localDelegatedProperties::get)
        if (localDelegatedProperties != null && localDelegatedProperties.isNotEmpty()) {
            state.bindingTrace.record(CodegenBinding.DELEGATED_PROPERTIES_WITH_METADATA, type, localDelegatedProperties.map { it.descriptor })
        }

        // TODO: if `-Xmultifile-parts-inherit` is enabled, write the corresponding flag for parts and facades to [Metadata.extraInt].
        var extraFlags = JvmAnnotationNames.METADATA_JVM_IR_FLAG
        if (state.isIrWithStableAbi) {
            extraFlags += JvmAnnotationNames.METADATA_JVM_IR_STABLE_ABI_FLAG
        }

        when (val metadata = irClass.metadata) {
            is MetadataSource.Class -> {
                val classProto = serializer!!.classProto(metadata.descriptor).build()
                writeKotlinMetadata(visitor, state, KotlinClassHeader.Kind.CLASS, extraFlags) {
                    AsmUtil.writeAnnotationData(it, serializer, classProto)
                }

                assert(irClass !in context.classNameOverride) {
                    "JvmPackageName is not supported for classes: ${irClass.render()}"
                }
            }
            is MetadataSource.File -> {
                val packageFqName = irClass.getPackageFragment()!!.fqName
                val packageProto = serializer!!.packagePartProto(packageFqName, metadata.descriptors)

                serializerExtension.serializeJvmPackage(packageProto, type)

                val facadeClassName = context.multifileFacadeForPart[irClass.attributeOwnerId]
                val kind = if (facadeClassName != null) KotlinClassHeader.Kind.MULTIFILE_CLASS_PART else KotlinClassHeader.Kind.FILE_FACADE
                writeKotlinMetadata(visitor, state, kind, extraFlags) { av ->
                    AsmUtil.writeAnnotationData(av, serializer, packageProto.build())

                    if (facadeClassName != null) {
                        av.visit(JvmAnnotationNames.METADATA_MULTIFILE_CLASS_NAME_FIELD_NAME, facadeClassName.internalName)
                    }

                    if (irClass in context.classNameOverride) {
                        av.visit(JvmAnnotationNames.METADATA_PACKAGE_NAME_FIELD_NAME, irClass.fqNameWhenAvailable!!.parent().asString())
                    }
                }
            }
            is MetadataSource.Function -> {
                val fakeDescriptor = createFreeFakeLambdaDescriptor(metadata.descriptor)
                val functionProto = serializer!!.functionProto(fakeDescriptor)?.build()
                writeKotlinMetadata(visitor, state, KotlinClassHeader.Kind.SYNTHETIC_CLASS, extraFlags) {
                    if (functionProto != null) {
                        AsmUtil.writeAnnotationData(it, serializer, functionProto)
                    }
                }
            }
            else -> {
                val entry = irClass.fileParent.fileEntry
                if (entry is MultifileFacadeFileEntry) {
                    val partInternalNames = entry.partFiles.mapNotNull { partFile ->
                        val fileClass = partFile.declarations.singleOrNull { it.isFileClass } as IrClass?
                        if (fileClass != null) typeMapper.mapClass(fileClass).internalName else null
                    }
                    MultifileClassCodegenImpl.writeMetadata(
                        visitor, state, extraFlags, partInternalNames, type, irClass.fqNameWhenAvailable!!.parent()
                    )
                } else {
                    writeSyntheticClassMetadata(visitor, state)
                }
            }
        }
    }

    private fun done() {
        writeInnerClasses()
        writeOuterClassAndEnclosingMethod()

        sourceMapper?.let {
            SourceMapper.flushToClassBuilder(it, visitor)
        }

        jvmSignatureClashDetector.reportErrors(classOrigin)

        visitor.done()
    }

    private fun IrFile.loadSourceFilesInfo(): List<File> {
        val entry = fileEntry
        if (entry is MultifileFacadeFileEntry) {
            return entry.partFiles.flatMap { it.loadSourceFilesInfo() }
        }
        return listOfNotNull(context.psiSourceManager.getFileEntry(this)?.let { File(it.name) })
    }

    companion object {
        fun generate(irClass: IrClass, context: JvmBackendContext) {
            ClassCodegen(irClass, context).generate()
        }

        private fun JvmClassSignature.hasInvalidName() =
            name.splitToSequence('/').any { identifier -> identifier.any { it in JvmSimpleNameBacktickChecker.INVALID_CHARS } }
    }

    private fun generateDeclaration(declaration: IrDeclaration) {
        when (declaration) {
            is IrField ->
                generateField(declaration)
            is IrFunction -> {
                generateMethod(declaration)
            }
            is IrAnonymousInitializer -> {
                // skip
            }
            is IrClass -> {
                // Nested classes are generated separately
            }
            else -> throw RuntimeException("Unsupported declaration $declaration")
        }
    }

    fun generateLocalClass(klass: IrClass, parentFunction: IrFunction): ReifiedTypeParametersUsages {
        return ClassCodegen(klass, context, this, parentFunction, withinInline = withinInline || parentFunction.isInline).generate()
    }

    private fun generateField(field: IrField) {
        if (field.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return

        val fieldType = typeMapper.mapType(field)
        val fieldSignature =
            if (field.origin == IrDeclarationOrigin.PROPERTY_DELEGATE) null
            else methodSignatureMapper.mapFieldSignature(field)
        val fieldName = field.name.asString()
        val fv = visitor.newField(
            field.OtherOrigin, field.flags, fieldName, fieldType.descriptor,
            fieldSignature, (field.initializer?.expression as? IrConst<*>)?.value
        )

        jvmSignatureClashDetector.trackField(field, RawSignature(fieldName, fieldType.descriptor, MemberKind.FIELD))

        object : AnnotationCodegen(this@ClassCodegen, context) {
            override fun visitAnnotation(descr: String?, visible: Boolean): AnnotationVisitor {
                return fv.visitAnnotation(descr, visible)
            }

            override fun visitTypeAnnotation(descr: String?, path: TypePath?, visible: Boolean): AnnotationVisitor {
                return fv.visitTypeAnnotation(TypeReference.newTypeReference(TypeReference.FIELD).value,path, descr, visible)
            }
        }.genAnnotations(field, fieldType, field.type)

        val descriptor = field.metadata?.descriptor
        if (descriptor != null) {
            state.globalSerializationBindings.put(JvmSerializationBindings.FIELD_FOR_PROPERTY, descriptor, fieldType to fieldName)
        }
    }

    private fun generateMethod(method: IrFunction) {
        jvmSignatureClashDetector.trackMethod(method)

        if (method.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return
        val signature = FunctionCodegen(method, this).generate().asmMethod

        when (val metadata = method.metadata) {
            is MetadataSource.Property -> {
                // We can't check for JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS because for interface methods
                // moved to DefaultImpls, origin is changed to DEFAULT_IMPLS
                // TODO: fix origin somehow, because otherwise $annotations methods in interfaces also don't have ACC_SYNTHETIC
                assert(method.name.asString().endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX)) { method.dump() }

                state.globalSerializationBindings.put(
                    JvmSerializationBindings.SYNTHETIC_METHOD_FOR_PROPERTY, metadata.descriptor, signature
                )
            }
            is MetadataSource.Function -> {
                visitor.serializationBindings.put(JvmSerializationBindings.METHOD_FOR_FUNCTION, metadata.descriptor, signature)
            }
            null -> {
            }
            else -> error("Incorrect metadata source $metadata for:\n${method.dump()}")
        }
    }

    private fun writeInnerClasses() {
        // JVMS7 (4.7.6): a nested class or interface member will have InnerClasses information
        // for each enclosing class and for each immediate member
        val classForInnerClassRecord = getClassForInnerClassRecord()
        if (classForInnerClassRecord != null) {
            parentClassCodegen?.innerClasses?.add(classForInnerClassRecord)

            var codegen: ClassCodegen? = this
            while (codegen != null) {
                val outerClass = codegen.getClassForInnerClassRecord()
                if (outerClass != null) {
                    innerClasses.add(outerClass)
                }
                codegen = codegen.parentClassCodegen
            }
        }

        for (innerClass in innerClasses) {
            writeInnerClass(innerClass, typeMapper, context, visitor)
        }
    }

    private fun getClassForInnerClassRecord(): IrClass? {
        return if (parentClassCodegen != null) irClass else null
    }

    // It's necessary for proper recovering of classId by plain string JVM descriptor when loading annotations
    // See FileBasedKotlinClass.convertAnnotationVisitor
    override fun addInnerClassInfoFromAnnotation(innerClass: IrClass) {
        var current: IrDeclaration? = innerClass
        while (current != null && !current.isTopLevelDeclaration) {
            if (current is IrClass) {
                innerClasses.add(current)
            }
            current = current.parent as? IrDeclaration
        }
    }

    private fun writeOuterClassAndEnclosingMethod() {
        // JVMS7 (4.7.7): A class must have an EnclosingMethod attribute if and only if
        // it is a local class or an anonymous class.
        //
        // The attribute contains the innermost class that encloses the declaration of
        // the current class. If the current class is immediately enclosed by a method
        // or constructor, the name and type of the function is recorded as well.
        if (parentClassCodegen != null) {
            val outerClassName = parentClassCodegen.type.internalName
            val enclosingFunction = context.customEnclosingFunction[irClass.attributeOwnerId] ?: parentFunction
            if (enclosingFunction != null) {
                val method = methodSignatureMapper.mapAsmMethod(enclosingFunction)
                visitor.visitOuterClass(outerClassName, method.name, method.descriptor)
            } else if (irClass.isAnonymousObject) {
                visitor.visitOuterClass(outerClassName, null, null)
            }
        }
    }

    fun getOrCreateSourceMapper(): DefaultSourceMapper {
        if (sourceMapper == null) {
            sourceMapper = context.getSourceMapper(irClass)
        }
        return sourceMapper!!
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
    get() = origin.flags or visibility.flags or (correspondingPropertySymbol?.owner?.deprecationFlags ?: 0) or
            (if (isFinal) Opcodes.ACC_FINAL else 0) or
            (if (isStatic) Opcodes.ACC_STATIC else 0) or
            (if (hasAnnotation(VOLATILE_ANNOTATION_FQ_NAME)) Opcodes.ACC_VOLATILE else 0) or
            (if (hasAnnotation(TRANSIENT_ANNOTATION_FQ_NAME)) Opcodes.ACC_TRANSIENT else 0) or
            (if (hasAnnotation(JVM_SYNTHETIC_ANNOTATION_FQ_NAME)) Opcodes.ACC_SYNTHETIC else 0)

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

private val Visibility.flags: Int
    get() = AsmUtil.getVisibilityAccessFlag(this) ?: throw AssertionError("Unsupported visibility $this")

internal val IrDeclaration.OtherOrigin: JvmDeclarationOrigin
    get() = OtherOrigin(descriptor)

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
