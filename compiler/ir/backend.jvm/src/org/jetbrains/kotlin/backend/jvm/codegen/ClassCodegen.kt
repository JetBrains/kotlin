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

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmDescriptorWithExtraFlags
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.inline.DefaultSourceMapper
import org.jetbrains.kotlin.codegen.inline.SourceMapper
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isTopLevelDeclaration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import java.io.File

open class ClassCodegen protected constructor(
    internal val irClass: IrClass,
    val context: JvmBackendContext,
    private val parentClassCodegen: ClassCodegen? = null
) : InnerClassConsumer {

    private val innerClasses = mutableListOf<ClassDescriptor>()

    val state = context.state

    val typeMapper = context.state.typeMapper

    val descriptor = irClass.descriptor

    private val isAnonymous = DescriptorUtils.isAnonymousObject(irClass.descriptor)

    val type: Type = if (isAnonymous) CodegenBinding.asmTypeForAnonymousClass(
        state.bindingContext,
        descriptor.source.getPsi() as KtElement
    ) else typeMapper.mapType(descriptor)

    private val sourceManager = context.psiSourceManager

    private val fileEntry = sourceManager.getFileEntry(irClass.fileParent)

    val psiElement = irClass.descriptor.psiElement

    val visitor: ClassBuilder = createClassBuilder()

    open fun createClassBuilder() = state.factory.newVisitor(
        OtherOrigin(psiElement, descriptor),
        type,
        psiElement?.containingFile?.let { setOf(it) } ?: emptySet()
    )

    private var sourceMapper: DefaultSourceMapper? = null

    private val serializerExtension = JvmSerializerExtension(visitor.serializationBindings, state)
    private val serializer: DescriptorSerializer? =
        when (val metadata = irClass.metadata) {
            is MetadataSource.Class -> DescriptorSerializer.create(metadata.descriptor, serializerExtension, parentClassCodegen?.serializer)
            is MetadataSource.File -> DescriptorSerializer.createTopLevel(serializerExtension)
            else -> null
        }

    fun generate() {
        val superClassInfo = SuperClassInfo.getSuperClassInfo(descriptor, typeMapper)
        val signature = ImplementationBodyCodegen.signature(descriptor, type, superClassInfo, typeMapper)

        visitor.defineClass(
            psiElement,
            state.classFileVersion,
            descriptor.calculateClassFlags(),
            signature.name,
            signature.javaGenericSignature,
            signature.superclassName,
            signature.interfaces.toTypedArray()
        )
        AnnotationCodegen.forClass(visitor.visitor, this, context.state).genAnnotations(descriptor, null)
        /* TODO: Temporary workaround: ClassBuilder needs a pathless name. */
        val shortName = File(fileEntry.name).name
        visitor.visitSource(shortName, null)

        val nestedClasses = irClass.declarations.mapNotNull { declaration ->
            if (declaration is IrClass) {
                ClassCodegen(declaration, context, this)
            } else null
        }

        val companionObjectCodegen = nestedClasses.firstOrNull { it.irClass.isCompanion }

        for (declaration in irClass.declarations) {
            generateDeclaration(declaration, companionObjectCodegen)
        }

        // Generate nested classes at the end, to ensure that codegen for companion object will have the necessary JVM signatures in its
        // trace for properties moved to the outer class
        for (codegen in nestedClasses) {
            codegen.generate()
        }

        generateKotlinMetadataAnnotation()

        done()
    }

    private fun generateKotlinMetadataAnnotation() {
        when (val metadata = irClass.metadata) {
            is MetadataSource.Class -> {
                val classProto = serializer!!.classProto(metadata.descriptor).build()
                writeKotlinMetadata(visitor, state, KotlinClassHeader.Kind.CLASS, 0) {
                    AsmUtil.writeAnnotationData(it, serializer, classProto)
                }
            }
            is MetadataSource.File -> {
                val packageFqName = irClass.getPackageFragment()!!.fqName
                val packageProto = serializer!!.packagePartProto(packageFqName, metadata.descriptors)

                serializerExtension.serializeJvmPackage(packageProto, type)

                writeKotlinMetadata(visitor, state, KotlinClassHeader.Kind.FILE_FACADE, 0) {
                    AsmUtil.writeAnnotationData(it, serializer, packageProto.build())
                    // TODO: JvmPackageName
                }
            }
            else -> {
                writeSyntheticClassMetadata(visitor, state)
            }
        }
    }

    private fun done() {
        writeInnerClasses()

        sourceMapper?.let {
            SourceMapper.flushToClassBuilder(it, visitor)
        }

        visitor.done()
    }

    companion object {
        fun generate(irClass: IrClass, context: JvmBackendContext) {
            val descriptor = irClass.descriptor
            val state = context.state

            if (ErrorUtils.isError(descriptor)) {
                badDescriptor(irClass, state.classBuilderMode)
                return
            }

            if (irClass.name == SpecialNames.NO_NAME_PROVIDED) {
                badDescriptor(irClass, state.classBuilderMode)
            }

            ClassCodegen(irClass, context).generate()
        }

        private fun badDescriptor(irClass: IrClass, mode: ClassBuilderMode) {
            if (mode.generateBodies) {
                throw IllegalStateException("Generating bad class in ClassBuilderMode = $mode: ${irClass.dump()}")
            }
        }
    }

    private fun generateDeclaration(declaration: IrDeclaration, companionObjectCodegen: ClassCodegen?) {
        when (declaration) {
            is IrField ->
                generateField(declaration, companionObjectCodegen)
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

    fun generateLocalClass(klass: IrClass) {
        ClassCodegen(klass, context, this).generate()
    }

    private fun generateField(field: IrField, companionObjectCodegen: ClassCodegen?) {
        if (field.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return

        val fieldType = typeMapper.mapType(field.descriptor)
        val fieldSignature = typeMapper.mapFieldSignature(field.descriptor.type, field.descriptor)
        val fieldName = field.descriptor.name.asString()
        val fv = visitor.newField(
            field.OtherOrigin, field.descriptor.calculateCommonFlags(), fieldName, fieldType.descriptor,
            fieldSignature, null/*TODO support default values*/
        )

        if (field.origin == IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY) {
            AnnotationCodegen.forField(fv, this, state).genAnnotations(field.descriptor, null)
        }

        val descriptor = field.metadata?.descriptor
        if (descriptor != null) {
            val codegen = if (JvmAbi.isPropertyWithBackingFieldInOuterClass(descriptor)) {
                companionObjectCodegen ?: error("Class with a property moved from the companion must have a companion:\n${irClass.dump()}")
            } else this
            codegen.visitor.serializationBindings.put(JvmSerializationBindings.FIELD_FOR_PROPERTY, descriptor, fieldType to fieldName)
        }
    }

    private fun generateMethod(method: IrFunction) {
        if (method.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return

        val signature = FunctionCodegen(method, this).generate().asmMethod

        val metadata = method.metadata
        when (metadata) {
            is MetadataSource.Property -> {
                // We can't check for JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS because for interface methods
                // moved to DefaultImpls, origin is changed to DEFAULT_IMPLS
                // TODO: fix origin somehow, because otherwise $annotations methods in interfaces also don't have ACC_SYNTHETIC
                assert(method.name.asString().endsWith(JvmAbi.ANNOTATED_PROPERTY_METHOD_NAME_SUFFIX)) { method.dump() }

                val codegen = if (DescriptorUtils.isInterface(metadata.descriptor.containingDeclaration)) {
                    assert(irClass.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS) { irClass.dump() }
                    parentClassCodegen!!
                } else {
                    this
                }
                codegen.visitor.serializationBindings.put(
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
        val classDescriptor = classForInnerClassRecord()
        if (classDescriptor != null) {
            parentClassCodegen?.innerClasses?.add(classDescriptor)

            var codegen: ClassCodegen? = this
            while (codegen != null) {
                val outerClass = codegen.classForInnerClassRecord()
                if (outerClass != null) {
                    innerClasses.add(outerClass)
                }
                codegen = codegen.parentClassCodegen
            }
        }

        for (innerClass in innerClasses) {
            MemberCodegen.writeInnerClass(innerClass, typeMapper, visitor)
        }
    }

    private fun classForInnerClassRecord(): ClassDescriptor? {
        return if (parentClassCodegen != null) descriptor else null
    }

    // It's necessary for proper recovering of classId by plain string JVM descriptor when loading annotations
    // See FileBasedKotlinClass.convertAnnotationVisitor
    override fun addInnerClassInfoFromAnnotation(classDescriptor: ClassDescriptor) {
        var current: DeclarationDescriptor? = classDescriptor
        while (current != null && !isTopLevelDeclaration(current)) {
            if (current is ClassDescriptor) {
                innerClasses.add(current)
            }
            current = current.containingDeclaration
        }
    }


    fun getOrCreateSourceMapper(): DefaultSourceMapper {
        if (sourceMapper == null) {
            sourceMapper = context.getSourceMapper(irClass)
        }
        return sourceMapper!!
    }

}

fun ClassDescriptor.calculateClassFlags(): Int {
    var flags = 0
    flags = flags or if (JvmCodegenUtil.isJvmInterface(this)) Opcodes.ACC_INTERFACE else Opcodes.ACC_SUPER
    flags = flags or calcModalityFlag()
    flags = flags or AsmUtil.getVisibilityAccessFlagForClass(this)
    flags = flags or if (kind == ClassKind.ENUM_CLASS) Opcodes.ACC_ENUM else 0
    flags = flags or if (kind == ClassKind.ANNOTATION_CLASS) Opcodes.ACC_ANNOTATION else 0
    return flags
}

fun MemberDescriptor.calculateCommonFlags(): Int {
    var flags = 0
    if (Visibilities.isPrivate(visibility)) {
        flags = flags.or(Opcodes.ACC_PRIVATE)
    } else if (visibility == Visibilities.PUBLIC || visibility == Visibilities.INTERNAL) {
        flags = flags.or(Opcodes.ACC_PUBLIC)
    } else if (visibility == Visibilities.PROTECTED) {
        flags = flags.or(Opcodes.ACC_PROTECTED)
    } else if (visibility == JavaVisibilities.PACKAGE_VISIBILITY) {
        // default visibility
    } else {
        throw RuntimeException("Unsupported visibility $visibility for descriptor $this")
    }

    flags = flags.or(calcModalityFlag())

    if (this is JvmDescriptorWithExtraFlags) {
        flags = flags or extraFlags
    }

    return flags
}

private fun MemberDescriptor.calcModalityFlag(): Int {
    var flags = 0
    if (this is PropertyDescriptor) {
        // Modality for a field: set FINAL for vals
        if (!isVar && !isLateInit) {
            flags = flags.or(Opcodes.ACC_FINAL)
        }
    } else when (effectiveModality) {
        Modality.ABSTRACT -> {
            flags = flags.or(Opcodes.ACC_ABSTRACT)
        }
        Modality.FINAL -> {
            if (this !is ConstructorDescriptor && !DescriptorUtils.isEnumClass(this)) {
                flags = flags.or(Opcodes.ACC_FINAL)
            }
        }
        Modality.OPEN -> {
            assert(!Visibilities.isPrivate(visibility))
        }
        else -> throw RuntimeException("Unsupported modality $modality for descriptor ${this}")
    }

    if (this is CallableMemberDescriptor) {
        if (this !is ConstructorDescriptor && dispatchReceiverParameter == null) {
            flags = flags or Opcodes.ACC_STATIC
        }
    }
    return flags
}

private val MemberDescriptor.effectiveModality: Modality
    get() {
        if (DescriptorUtils.isSealedClass(this) ||
            DescriptorUtils.isAnnotationClass(this)
        ) {
            return Modality.ABSTRACT
        }

        return modality
    }

private val IrField.OtherOrigin: JvmDeclarationOrigin
    get() = OtherOrigin(descriptor.psiElement, this.descriptor)

internal val IrFunction.OtherOrigin: JvmDeclarationOrigin
    get() = OtherOrigin(descriptor.psiElement, this.descriptor)
