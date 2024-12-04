/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.serialization

import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.createFreeFakeLocalPropertyDescriptor
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapperBase
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.DescriptorsJvmAbiUtil
import org.jetbrains.kotlin.load.java.lazy.types.RawTypeImpl
import org.jetbrains.kotlin.load.kotlin.NON_EXISTENT_CLASS_NAME
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.ClassMapperLite
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmFlags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.metadata.serialization.StringTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isInterface
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.nonSourceAnnotations
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmDefaultNoCompatibilityAnnotation
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmDefaultWithCompatibilityAnnotation
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForParameterTypes
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForReturnType
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.DescriptorSerializer.Companion.writeVersionRequirement
import org.jetbrains.kotlin.serialization.SerializerExtension
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type

class JvmSerializerExtension @JvmOverloads constructor(
    private val bindings: JvmSerializationBindings,
    state: GenerationState,
    private val typeMapper: KotlinTypeMapperBase = state.typeMapper
) : SerializerExtension() {
    private val globalBindings = state.globalSerializationBindings
    private val codegenBinding = state.bindingContext
    override val stringTable = JvmCodegenStringTable(typeMapper)
    private val useTypeTable = state.config.useTypeTableInSerializer
    private val moduleName = state.moduleName
    private val classBuilderMode = state.classBuilderMode
    private val languageVersionSettings = state.languageVersionSettings
    private val isParamAssertionsDisabled = state.config.isParamAssertionsDisabled
    private val unifiedNullChecks = state.config.unifiedNullChecks
    private val functionsWithInlineClassReturnTypesMangled = state.config.functionsWithInlineClassReturnTypesMangled
    override val metadataVersion = state.config.metadataVersion
    private val jvmDefaultMode = state.jvmDefaultMode
    private val approximator = state.typeApproximator
    private val useOldManglingScheme = state.config.useOldManglingSchemeForFunctionsWithInlineClassesInSignatures
    private val signatureSerializer = JvmSignatureSerializerImpl(stringTable)

    override fun shouldUseTypeTable(): Boolean = useTypeTable

    override fun serializeClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer
    ) {
        if (moduleName != JvmProtoBufUtil.DEFAULT_MODULE_NAME) {
            proto.setExtension(JvmProtoBuf.classModuleName, stringTable.getStringIndex(moduleName))
        }
        //TODO: support local delegated properties in new defaults scheme
        val containerAsmType =
            if (isInterface(descriptor) && !jvmDefaultMode.isEnabled) typeMapper.mapDefaultImpls(descriptor) else typeMapper.mapClass(descriptor)
        writeLocalProperties(proto, containerAsmType, JvmProtoBuf.classLocalVariable)
        writeVersionRequirementForJvmDefaultIfNeeded(descriptor, proto, versionRequirementTable)

        if (jvmDefaultMode.isEnabled && isInterface(descriptor)) {
            proto.setExtension(
                JvmProtoBuf.jvmClassFlags,
                JvmFlags.getClassFlags(
                    true,
                    (JvmDefaultMode.ALL_COMPATIBILITY == jvmDefaultMode && !descriptor.hasJvmDefaultNoCompatibilityAnnotation()) ||
                            (JvmDefaultMode.ALL == jvmDefaultMode && descriptor.hasJvmDefaultWithCompatibilityAnnotation())
                )
            )
        }
    }

    // Interfaces which have @JvmDefault members somewhere in the hierarchy need the compiler 1.2.40+
    // so that the generated bridges in subclasses would call the super members correctly
    private fun writeVersionRequirementForJvmDefaultIfNeeded(
        classDescriptor: ClassDescriptor,
        builder: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable
    ) {
        if (isInterface(classDescriptor)) {
            if (jvmDefaultMode == JvmDefaultMode.ALL) {
                builder.addVersionRequirement(
                    writeVersionRequirement(1, 4, 0, ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, versionRequirementTable)
                )
            }
        }
    }

    override fun serializePackage(packageFqName: FqName, proto: ProtoBuf.Package.Builder) {
        if (moduleName != JvmProtoBufUtil.DEFAULT_MODULE_NAME) {
            proto.setExtension(JvmProtoBuf.packageModuleName, stringTable.getStringIndex(moduleName))
        }
    }

    fun serializeJvmPackage(proto: ProtoBuf.Package.Builder, partAsmType: Type) {
        writeLocalProperties(proto, partAsmType, JvmProtoBuf.packageLocalVariable)
    }

    private fun <MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>, BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>> writeLocalProperties(
        proto: BuilderType,
        classAsmType: Type,
        extension: GeneratedMessageLite.GeneratedExtension<MessageType, List<ProtoBuf.Property>>
    ) {
        val localVariables = CodegenBinding.getLocalDelegatedProperties(codegenBinding, classAsmType) ?: return

        for (localVariable in localVariables) {
            val propertyDescriptor = createFreeFakeLocalPropertyDescriptor(localVariable, approximator)
            val serializer = DescriptorSerializer.createForLambda(this, languageVersionSettings)
            proto.addExtension(extension, serializer.propertyProto(propertyDescriptor)?.build() ?: continue)
        }
    }

    override fun serializeFlexibleType(
        flexibleType: FlexibleType,
        lowerProto: ProtoBuf.Type.Builder,
        upperProto: ProtoBuf.Type.Builder
    ) {
        lowerProto.flexibleTypeCapabilitiesId = stringTable.getStringIndex(JvmProtoBufUtil.PLATFORM_TYPE_ID)

        if (flexibleType is RawTypeImpl) {
            lowerProto.setExtension(JvmProtoBuf.isRaw, true)

            // we write this Extension for compatibility with old compiler
            upperProto.setExtension(JvmProtoBuf.isRaw, true)
        }
    }

    override fun serializeType(type: KotlinType, proto: ProtoBuf.Type.Builder) {
        // TODO: don't store type annotations in our binary metadata on Java 8, use *TypeAnnotations attributes instead
        for (annotation in type.nonSourceAnnotations) {
            proto.addExtension(JvmProtoBuf.typeAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
        for (annotation in typeParameter.nonSourceAnnotations) {
            proto.addExtension(JvmProtoBuf.typeParameterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeConstructor(
        descriptor: ConstructorDescriptor, proto: ProtoBuf.Constructor.Builder, childSerializer: DescriptorSerializer
    ) {
        val method = getBinding(METHOD_FOR_FUNCTION, descriptor)
        if (method != null) {
            val signature = signatureSerializer.methodSignature(descriptor, descriptor.name, method)
            if (signature != null) {
                proto.setExtension(JvmProtoBuf.constructorSignature, signature)
            }
        }
    }

    override fun serializeFunction(
        descriptor: FunctionDescriptor,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
        val method = getBinding(METHOD_FOR_FUNCTION, descriptor)
        if (method != null) {
            val signature = signatureSerializer.methodSignature(descriptor, descriptor.name, method)
            if (signature != null) {
                proto.setExtension(JvmProtoBuf.methodSignature, signature)
            }
        }

        if (descriptor.needsInlineParameterNullCheckRequirement()) {
            versionRequirementTable?.writeInlineParameterNullCheckRequirement(proto::addVersionRequirement)
        }

        if (requiresFunctionNameManglingForReturnType(descriptor) &&
            !DescriptorUtils.hasJvmNameAnnotation(descriptor) &&
            !requiresFunctionNameManglingForParameterTypes(descriptor)
        ) {
            versionRequirementTable?.writeFunctionNameManglingForReturnTypeRequirement(proto::addVersionRequirement)
        }

        if ((requiresFunctionNameManglingForReturnType(descriptor) ||
                    requiresFunctionNameManglingForParameterTypes(descriptor)) &&
            !DescriptorUtils.hasJvmNameAnnotation(descriptor) && !useOldManglingScheme
        ) {
            versionRequirementTable?.writeNewFunctionNameManglingRequirement(proto::addVersionRequirement)
        }
    }

    private fun MutableVersionRequirementTable.writeInlineParameterNullCheckRequirement(add: (Int) -> Unit) {
        if (unifiedNullChecks) {
            // Since Kotlin 1.4, we generate a call to Intrinsics.checkNotNullParameter in inline functions which causes older compilers
            // (earlier than 1.3.50) to crash because a functional parameter in this position can't be inlined
            add(writeVersionRequirement(1, 3, 50, ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, this))
        }
    }

    private fun MutableVersionRequirementTable.writeFunctionNameManglingForReturnTypeRequirement(add: (Int) -> Unit) {
        if (functionsWithInlineClassReturnTypesMangled) {
            add(writeVersionRequirement(1, 4, 0, ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION, this))
        }
    }

    private fun MutableVersionRequirementTable.writeNewFunctionNameManglingRequirement(add: (Int) -> Unit) {
        add(writeVersionRequirement(1, 4, 30, ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, this))
    }

    private fun FunctionDescriptor.needsInlineParameterNullCheckRequirement(): Boolean =
        isInline && !isSuspend && !isParamAssertionsDisabled &&
                !DescriptorVisibilities.isPrivate(visibility) &&
                (valueParameters.any { it.type.isFunctionType } || extensionReceiverParameter?.type?.isFunctionType == true)

    override fun serializeProperty(
        descriptor: PropertyDescriptor,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
        val getter = descriptor.getter
        val setter = descriptor.setter
        val getterMethod = if (getter == null) null else getBinding(METHOD_FOR_FUNCTION, getter)
        val setterMethod = if (setter == null) null else getBinding(METHOD_FOR_FUNCTION, setter)

        val field = getBinding(FIELD_FOR_PROPERTY, descriptor)
        val syntheticMethod = getBinding(SYNTHETIC_METHOD_FOR_PROPERTY, descriptor)
        val delegateMethod = getBinding(DELEGATE_METHOD_FOR_PROPERTY, descriptor)
        assert(descriptor.isDelegated || delegateMethod == null) { "non-delegated property $descriptor has delegate method" }

        val signature = signatureSerializer.propertySignature(
            descriptor.name,
            field?.second,
            field?.first?.descriptor,
            if (syntheticMethod != null) signatureSerializer.methodSignature(null, null, syntheticMethod) else null,
            if (delegateMethod != null) signatureSerializer.methodSignature(null, null, delegateMethod) else null,
            if (getterMethod != null) signatureSerializer.methodSignature(null, null, getterMethod) else null,
            if (setterMethod != null) signatureSerializer.methodSignature(null, null, setterMethod) else null,
            field?.first?.descriptor?.let { signatureSerializer.requiresPropertySignature(descriptor, it) } ?: false,
        )

        if (signature != null) {
            proto.setExtension(JvmProtoBuf.propertySignature, signature)
        }

        if (descriptor.isJvmFieldPropertyInInterfaceCompanion() && versionRequirementTable != null) {
            proto.setExtension(JvmProtoBuf.flags, JvmFlags.getPropertyFlags(true))
        }

        if (getter?.needsInlineParameterNullCheckRequirement() == true || setter?.needsInlineParameterNullCheckRequirement() == true) {
            versionRequirementTable?.writeInlineParameterNullCheckRequirement(proto::addVersionRequirement)
        }

        if (!DescriptorUtils.hasJvmNameAnnotation(descriptor) && requiresFunctionNameManglingForReturnType(descriptor)) {
            if (!useOldManglingScheme) {
                versionRequirementTable?.writeNewFunctionNameManglingRequirement(proto::addVersionRequirement)
            }
            versionRequirementTable?.writeFunctionNameManglingForReturnTypeRequirement(proto::addVersionRequirement)
        }
    }

    private fun PropertyDescriptor.isJvmFieldPropertyInInterfaceCompanion(): Boolean {
        if (!DescriptorsJvmAbiUtil.hasJvmFieldAnnotation(this)) return false

        val container = containingDeclaration
        if (!DescriptorUtils.isCompanionObject(container)) return false

        val grandParent = (container as ClassDescriptor).containingDeclaration
        return isInterface(grandParent) || DescriptorUtils.isAnnotationClass(grandParent)
    }

    override fun serializeErrorType(type: KotlinType, builder: ProtoBuf.Type.Builder) {
        if (classBuilderMode === ClassBuilderMode.KAPT3) {
            builder.className = stringTable.getStringIndex(NON_EXISTENT_CLASS_NAME)
            return
        }

        super.serializeErrorType(type, builder)
    }

    private fun <K : Any, V> getBinding(slice: SerializationMappingSlice<K, V>, key: K): V? =
        bindings.get(slice, key) ?: globalBindings.get(slice, key)
}

class JvmSignatureSerializerImpl(stringTable: StringTable) : JvmSignatureSerializer<FunctionDescriptor, PropertyDescriptor>(stringTable) {
    // We don't write those signatures which can be trivially reconstructed from already serialized data
    // TODO: make JvmStringTable implement NameResolver and use JvmProtoBufUtil#getJvmMethodSignature instead
    override fun requiresFunctionSignature(descriptor: FunctionDescriptor, desc: String): Boolean {
        val sb = StringBuilder()
        sb.append("(")
        val receiverParameter = descriptor.extensionReceiverParameter
        if (receiverParameter != null) {
            val receiverDesc = mapTypeDefault(receiverParameter.value.type) ?: return true
            sb.append(receiverDesc)
        }

        for (valueParameter in descriptor.valueParameters) {
            val paramDesc = mapTypeDefault(valueParameter.type) ?: return true
            sb.append(paramDesc)
        }

        sb.append(")")

        val returnType = descriptor.returnType
        val returnTypeDesc = (if (returnType == null) "V" else mapTypeDefault(returnType)) ?: return true
        sb.append(returnTypeDesc)

        return sb.toString() != desc
    }

    override fun requiresPropertySignature(descriptor: PropertyDescriptor, desc: String): Boolean {
        return desc != mapTypeDefault(descriptor.type)
    }

    private fun mapTypeDefault(type: KotlinType): String? {
        val classifier = type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
        val classId = classifier.classId
        return if (classId == null) null else ClassMapperLite.mapClass(classId.asString())
    }
}

