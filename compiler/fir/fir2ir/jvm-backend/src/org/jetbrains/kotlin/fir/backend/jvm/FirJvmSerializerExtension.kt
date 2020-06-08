/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapperBase
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirEffectiveVisibilityImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.serialization.FirSerializerExtension
import org.jetbrains.kotlin.fir.serialization.nonSourceAnnotations
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.NON_EXISTENT_CLASS_NAME
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.ClassMapperLite
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmFlags
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.annotations.JVM_DEFAULT_FQ_NAME
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class FirJvmSerializerExtension @JvmOverloads constructor(
    override val session: FirSession,
    private val bindings: JvmSerializationBindings,
    state: GenerationState,
    private val irClass: IrClass,
    private val typeMapper: KotlinTypeMapperBase = state.typeMapper
) : FirSerializerExtension() {
    private val globalBindings = state.globalSerializationBindings
    override val stringTable = FirJvmElementAwareStringTable(typeMapper)
    private val useTypeTable = state.useTypeTableInSerializer
    private val moduleName = state.moduleName
    private val classBuilderMode = state.classBuilderMode
    private val isParamAssertionsDisabled = state.isParamAssertionsDisabled
    private val unifiedNullChecks = state.unifiedNullChecks
    override val metadataVersion = state.metadataVersion
    private val jvmDefaultMode = state.jvmDefaultMode

    override fun shouldUseTypeTable(): Boolean = useTypeTable
    override fun shouldSerializeFunction(function: FirFunction<*>): Boolean {
        return classBuilderMode != ClassBuilderMode.ABI ||
                function !is FirSimpleFunction || function.visibility != Visibilities.PRIVATE
    }

    override fun shouldSerializeProperty(property: FirProperty): Boolean {
        return classBuilderMode != ClassBuilderMode.ABI || property.visibility != Visibilities.PRIVATE
    }

    override fun shouldSerializeTypeAlias(typeAlias: FirTypeAlias): Boolean {
        return classBuilderMode != ClassBuilderMode.ABI || typeAlias.visibility != Visibilities.PRIVATE
    }

    override fun shouldSerializeNestedClass(nestedClass: FirRegularClass): Boolean {
        return classBuilderMode != ClassBuilderMode.ABI || nestedClass.effectiveVisibility != FirEffectiveVisibilityImpl.Private
    }

    @OptIn(DescriptorBasedIr::class)
    override fun serializeClass(
        klass: FirClass<*>,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: FirElementSerializer
    ) {
        assert((irClass.metadata as FirMetadataSource.Class).klass == klass)
        if (moduleName != JvmProtoBufUtil.DEFAULT_MODULE_NAME) {
            proto.setExtension(JvmProtoBuf.classModuleName, stringTable.getStringIndex(moduleName))
        }
        //TODO: support local delegated properties in new defaults scheme
        val containerAsmType =
            if (klass is FirRegularClass && klass.classKind == ClassKind.INTERFACE) typeMapper.mapDefaultImpls(irClass.descriptor)
            else typeMapper.mapClass(irClass.descriptor)
        writeLocalProperties(proto, containerAsmType, JvmProtoBuf.classLocalVariable)
        writeVersionRequirementForJvmDefaultIfNeeded(klass, proto, versionRequirementTable)

        if (jvmDefaultMode.forAllMethodsWithBody && klass is FirRegularClass && klass.classKind == ClassKind.INTERFACE) {
            proto.setExtension(JvmProtoBuf.jvmClassFlags, JvmFlags.getClassFlags(true))
        }
    }

    // Interfaces which have @JvmDefault members somewhere in the hierarchy need the compiler 1.2.40+
    // so that the generated bridges in subclasses would call the super members correctly
    private fun writeVersionRequirementForJvmDefaultIfNeeded(
        klass: FirClass<*>,
        builder: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable
    ) {
        if (klass is FirRegularClass && klass.classKind == ClassKind.INTERFACE) {
            if (jvmDefaultMode == JvmDefaultMode.ENABLE && klass.declarations.any {
                    it is FirCallableMemberDeclaration<*> && it.annotations.any { annotationCall ->
                        val classId = annotationCall.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.classId
                        classId?.asSingleFqName() == JVM_DEFAULT_FQ_NAME
                    }
                }) {
                builder.addVersionRequirement(
                    DescriptorSerializer.writeVersionRequirement(
                        1,
                        2,
                        40,
                        ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION,
                        versionRequirementTable
                    )
                )
            }
            if (jvmDefaultMode == JvmDefaultMode.ALL_INCOMPATIBLE) {
                builder.addVersionRequirement(
                    DescriptorSerializer.writeVersionRequirement(
                        1,
                        4,
                        0,
                        ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION,
                        versionRequirementTable
                    )
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
        // TODO
//        val localVariables = CodegenBinding.getLocalDelegatedProperties(codegenBinding, classAsmType) ?: return
//
//        for (localVariable in localVariables) {
//            val propertyDescriptor = createFreeFakeLocalPropertyDescriptor(localVariable)
//            val serializer = FirElementSerializer.createForLambda(this)
//            proto.addExtension(extension, serializer.propertyProto(propertyDescriptor)?.build() ?: continue)
//        }
    }

    override fun serializeFlexibleType(
        type: ConeFlexibleType,
        lowerProto: ProtoBuf.Type.Builder,
        upperProto: ProtoBuf.Type.Builder
    ) {
        lowerProto.flexibleTypeCapabilitiesId = stringTable.getStringIndex(JvmProtoBufUtil.PLATFORM_TYPE_ID)

        if (type is ConeRawType) {
            lowerProto.setExtension(JvmProtoBuf.isRaw, true)

            // we write this Extension for compatibility with old compiler
            upperProto.setExtension(JvmProtoBuf.isRaw, true)
        }
    }

    override fun serializeType(type: ConeKotlinType, proto: ProtoBuf.Type.Builder) {
        // TODO: don't store type annotations in our binary metadata on Java 8, use *TypeAnnotations attributes instead
        // TODO: (FIR) ConeKotlinType does not store annotations, see KT-30066
//        for (annotation in type.annotations) {
//            proto.addExtension(JvmProtoBuf.typeAnnotation, annotationSerializer.serializeAnnotation(annotation))
//        }
    }

    override fun serializeTypeParameter(typeParameter: FirTypeParameter, proto: ProtoBuf.TypeParameter.Builder) {
        for (annotation in typeParameter.nonSourceAnnotations(session)) {
            proto.addExtension(JvmProtoBuf.typeParameterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeConstructor(
        constructor: FirConstructor, proto: ProtoBuf.Constructor.Builder, childSerializer: FirElementSerializer
    ) {
        val method = getBinding(METHOD_FOR_FIR_FUNCTION, constructor)
        if (method != null) {
            val signature = SignatureSerializer().methodSignature(constructor, method)
            if (signature != null) {
                proto.setExtension(JvmProtoBuf.constructorSignature, signature)
            }
        }
    }

    override fun serializeFunction(
        function: FirFunction<*>,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
        val method = getBinding(METHOD_FOR_FIR_FUNCTION, function)
        if (method != null) {
            val signature = SignatureSerializer().methodSignature(function, method)
            if (signature != null) {
                proto.setExtension(JvmProtoBuf.methodSignature, signature)
            }
        }

        if (function.needsInlineParameterNullCheckRequirement()) {
            versionRequirementTable?.writeInlineParameterNullCheckRequirement(proto::addVersionRequirement)
        }
    }

    private fun MutableVersionRequirementTable.writeInlineParameterNullCheckRequirement(add: (Int) -> Unit) {
        if (unifiedNullChecks) {
            // Since Kotlin 1.4, we generate a call to Intrinsics.checkNotNullParameter in inline functions which causes older compilers
            // (earlier than 1.3.50) to crash because a functional parameter in this position can't be inlined
            add(DescriptorSerializer.writeVersionRequirement(1, 3, 50, ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION, this))
        }
    }

    private fun FirFunction<*>.needsInlineParameterNullCheckRequirement(): Boolean =
        this is FirSimpleFunction && isInline && !isSuspend && !isParamAssertionsDisabled &&
                !Visibilities.isPrivate(visibility) &&
                (valueParameters.any { it.returnTypeRef.coneTypeSafe<ConeKotlinType>()?.isBuiltinFunctionalType(session) == true } ||
                        receiverTypeRef?.coneTypeSafe<ConeKotlinType>()?.isBuiltinFunctionalType(session) == true)

    override fun serializeProperty(
        property: FirProperty,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
        val signatureSerializer = SignatureSerializer()

        val getter = property.getter
        val setter = property.setter
        val getterMethod = if (getter == null) null else getBinding(METHOD_FOR_FIR_FUNCTION, getter)
        val setterMethod = if (setter == null) null else getBinding(METHOD_FOR_FIR_FUNCTION, setter)

        val field = getBinding(FIELD_FOR_PROPERTY, property)
        val syntheticMethod = getBinding(SYNTHETIC_METHOD_FOR_FIR_VARIABLE, property)

        val signature = signatureSerializer.propertySignature(
            property,
            field?.second,
            field?.first?.descriptor,
            if (syntheticMethod != null) signatureSerializer.methodSignature(null, syntheticMethod) else null,
            if (getterMethod != null) signatureSerializer.methodSignature(null, getterMethod) else null,
            if (setterMethod != null) signatureSerializer.methodSignature(null, setterMethod) else null
        )

        if (signature != null) {
            proto.setExtension(JvmProtoBuf.propertySignature, signature)
        }

        // TODO
//        if (property.isJvmFieldPropertyInInterfaceCompanion() && versionRequirementTable != null) {
//            proto.setExtension(JvmProtoBuf.flags, JvmFlags.getPropertyFlags(true))
//
//            proto.addVersionRequirement(
//                DescriptorSerializer.writeVersionRequirement(
//                    1,
//                    2,
//                    70,
//                    ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION,
//                    versionRequirementTable
//                )
//            )
//        }

        if (getter?.needsInlineParameterNullCheckRequirement() == true || setter?.needsInlineParameterNullCheckRequirement() == true) {
            versionRequirementTable?.writeInlineParameterNullCheckRequirement(proto::addVersionRequirement)
        }
    }

    private fun PropertyDescriptor.isJvmFieldPropertyInInterfaceCompanion(): Boolean {
        if (!JvmAbi.hasJvmFieldAnnotation(this)) return false

        val container = containingDeclaration
        if (!DescriptorUtils.isCompanionObject(container)) return false

        val grandParent = (container as ClassDescriptor).containingDeclaration
        return DescriptorUtils.isInterface(grandParent) || DescriptorUtils.isAnnotationClass(grandParent)
    }

    override fun serializeErrorType(type: ConeKotlinErrorType, builder: ProtoBuf.Type.Builder) {
        if (classBuilderMode === ClassBuilderMode.KAPT3) {
            builder.className = stringTable.getStringIndex(NON_EXISTENT_CLASS_NAME)
            return
        }

        super.serializeErrorType(type, builder)
    }

    private fun <K, V> getBinding(slice: JvmSerializationBindings.SerializationMappingSlice<K, V>, key: K): V? =
        bindings.get(slice, key) ?: globalBindings.get(slice, key)

    private inner class SignatureSerializer {
        fun methodSignature(function: FirFunction<*>?, method: Method): JvmProtoBuf.JvmMethodSignature? {
            val builder = JvmProtoBuf.JvmMethodSignature.newBuilder()
            if (function == null || (function as? FirSimpleFunction)?.name?.asString() != method.name) {
                builder.name = stringTable.getStringIndex(method.name)
            }
            if (function == null || requiresSignature(function, method.descriptor)) {
                builder.desc = stringTable.getStringIndex(method.descriptor)
            }
            return if (builder.hasName() || builder.hasDesc()) builder.build() else null
        }

        // We don't write those signatures which can be trivially reconstructed from already serialized data
        // TODO: make JvmStringTable implement NameResolver and use JvmProtoBufUtil#getJvmMethodSignature instead
        private fun requiresSignature(function: FirFunction<*>, desc: String): Boolean {
            val sb = StringBuilder()
            sb.append("(")
            val receiverTypeRef = function.receiverTypeRef
            if (receiverTypeRef != null) {
                val receiverDesc = mapTypeDefault(receiverTypeRef) ?: return true
                sb.append(receiverDesc)
            }

            for (valueParameter in function.valueParameters) {
                val paramDesc = mapTypeDefault(valueParameter.returnTypeRef) ?: return true
                sb.append(paramDesc)
            }

            sb.append(")")

            val returnTypeRef = function.returnTypeRef
            val returnTypeDesc = (mapTypeDefault(returnTypeRef)) ?: return true
            sb.append(returnTypeDesc)

            return sb.toString() != desc
        }

        private fun requiresSignature(property: FirProperty, desc: String): Boolean {
            return desc != mapTypeDefault(property.returnTypeRef)
        }

        private fun mapTypeDefault(typeRef: FirTypeRef): String? {
            val classId = typeRef.coneTypeSafe<ConeClassLikeType>()?.classId
            return if (classId == null) null else ClassMapperLite.mapClass(classId.asString())
        }

        fun propertySignature(
            property: FirProperty,
            fieldName: String?,
            fieldDesc: String?,
            syntheticMethod: JvmProtoBuf.JvmMethodSignature?,
            getter: JvmProtoBuf.JvmMethodSignature?,
            setter: JvmProtoBuf.JvmMethodSignature?
        ): JvmProtoBuf.JvmPropertySignature? {
            val signature = JvmProtoBuf.JvmPropertySignature.newBuilder()

            if (fieldDesc != null) {
                assert(fieldName != null) { "Field name shouldn't be null when there's a field type: $fieldDesc" }
                signature.field = fieldSignature(property, fieldName!!, fieldDesc)
            }

            if (syntheticMethod != null) {
                signature.syntheticMethod = syntheticMethod
            }

            if (getter != null) {
                signature.getter = getter
            }
            if (setter != null) {
                signature.setter = setter
            }

            return signature.build().takeIf { it.serializedSize > 0 }
        }

        fun fieldSignature(
            property: FirProperty,
            name: String,
            desc: String
        ): JvmProtoBuf.JvmFieldSignature {
            val builder = JvmProtoBuf.JvmFieldSignature.newBuilder()
            if (property.name.asString() != name) {
                builder.name = stringTable.getStringIndex(name)
            }
            if (requiresSignature(property, desc)) {
                builder.desc = stringTable.getStringIndex(desc)
            }
            return builder.build()
        }
    }

    companion object {
        val METHOD_FOR_FIR_FUNCTION = JvmSerializationBindings.SerializationMappingSlice.create<FirFunction<*>, Method>()
        val FIELD_FOR_PROPERTY = JvmSerializationBindings.SerializationMappingSlice.create<FirProperty, Pair<Type, String>>()
        val SYNTHETIC_METHOD_FOR_FIR_VARIABLE = JvmSerializationBindings.SerializationMappingSlice.create<FirVariable<*>, Method>()
    }

}
