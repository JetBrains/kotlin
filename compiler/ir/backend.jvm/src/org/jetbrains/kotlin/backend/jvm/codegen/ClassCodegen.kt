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
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.inline.DefaultSourceMapper
import org.jetbrains.kotlin.codegen.inline.SourceMapper
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
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
            irClass.flags,
            signature.name,
            signature.javaGenericSignature,
            signature.superclassName,
            signature.interfaces.toTypedArray()
        )
        AnnotationCodegen(this, context.state, visitor.visitor::visitAnnotation).genAnnotations(irClass, null)
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
        writeOuterClassAndEnclosingMethod()

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
            field.OtherOrigin, field.flags, fieldName, fieldType.descriptor,
            fieldSignature, null/*TODO support default values*/
        )

        AnnotationCodegen(this, state, fv::visitAnnotation).genAnnotations(field, fieldType)

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
    override fun addInnerClassInfoFromAnnotation(irClass: IrClass) {
        var current: DeclarationDescriptor? = irClass.descriptor
        while (current != null && !isTopLevelDeclaration(current)) {
            if (current is ClassDescriptor) {
                innerClasses.add(current)
            }
            current = current.containingDeclaration
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
            // TODO: Since the class could have been reparented in lowerings, this could
            // be a class instead of the actual function that the class is nested inside
            // in the source.
            val containingDeclaration = irClass.symbol.owner.parent
            if (containingDeclaration is IrFunction) {
                val method = typeMapper.mapAsmMethod(containingDeclaration.descriptor)
                visitor.visitOuterClass(outerClassName, method.name, method.descriptor)
            } else {
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
    get() = origin.flags or AsmUtil.getVisibilityAccessFlagForClass(descriptor) or when {
        isAnnotationClass -> Opcodes.ACC_ANNOTATION or Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
        isInterface -> Opcodes.ACC_INTERFACE or Opcodes.ACC_ABSTRACT
        isEnumClass -> Opcodes.ACC_ENUM or Opcodes.ACC_SUPER or modality.flags
        else -> Opcodes.ACC_SUPER or modality.flags
    }

private val IrField.flags: Int
    get() = origin.flags or visibility.flags or
            (if (isFinal) Opcodes.ACC_FINAL else 0) or
            (if (isStatic) Opcodes.ACC_STATIC else 0)

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

private val IrField.OtherOrigin: JvmDeclarationOrigin
    get() = OtherOrigin(descriptor.psiElement, this.descriptor)

internal val IrFunction.OtherOrigin: JvmDeclarationOrigin
    get() = OtherOrigin(descriptor.psiElement, this.descriptor)
