/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.serialization.constant.toConstantValue
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

abstract class FirSerializerExtensionBase(
    val protocol: SerializerExtensionProtocol,
) : FirSerializerExtension() {
    final override val stringTable = FirElementAwareSerializableStringTable()

    override fun serializePackage(packageFqName: FqName, proto: ProtoBuf.Package.Builder) {
        proto.setExtension(protocol.packageFqName, stringTable.getPackageFqNameIndex(packageFqName))
    }

    override fun serializeClass(
        klass: FirClass,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: FirElementSerializer
    ) {
        klass.serializeAnnotations(proto, protocol.classAnnotation)
    }

    override fun serializeConstructor(
        constructor: FirConstructor,
        proto: ProtoBuf.Constructor.Builder,
        childSerializer: FirElementSerializer
    ) {
        constructor.serializeAnnotations(proto, protocol.constructorAnnotation)
    }

    override fun serializeFunction(
        function: FirFunction,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
        function.serializeAnnotations(proto, protocol.functionAnnotation)
        function.receiverParameter?.serializeAnnotations(proto, protocol.functionExtensionReceiverAnnotation)
    }

    override fun serializeProperty(
        property: FirProperty,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
        val fieldPropertyAnnotations = mutableListOf<FirAnnotation>()
        val delegatePropertyAnnotations = mutableListOf<FirAnnotation>()

        for (annotation in property.backingField?.nonSourceAnnotations(session).orEmpty()) {
            val destination = when (annotation.useSiteTarget) {
                AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> delegatePropertyAnnotations
                else -> fieldPropertyAnnotations
            }
            destination += annotation
        }

        property.nonSourceAnnotations(session).serializeAnnotations(proto, protocol.propertyAnnotation)
        fieldPropertyAnnotations.serializeAnnotations(proto, protocol.propertyBackingFieldAnnotation)
        delegatePropertyAnnotations.serializeAnnotations(proto, protocol.propertyDelegatedFieldAnnotation)

        property.getter?.serializeAnnotations(proto, protocol.propertyGetterAnnotation)
        property.setter?.serializeAnnotations(proto, protocol.propertySetterAnnotation)
        property.receiverParameter?.serializeAnnotations(proto, protocol.propertyExtensionReceiverAnnotation)

        if (!Flags.HAS_CONSTANT.get(proto.flags)) return
        property.initializer?.toConstantValue<ConstantValue<*>>(session, constValueProvider)?.let {
            proto.setExtension(protocol.compileTimeValue, annotationSerializer.valueProto(it).build())
        }
    }

    override fun serializeEnumEntry(enumEntry: FirEnumEntry, proto: ProtoBuf.EnumEntry.Builder) {
        enumEntry.serializeAnnotations(proto, protocol.enumEntryAnnotation)
    }

    override fun serializeValueParameter(parameter: FirValueParameter, proto: ProtoBuf.ValueParameter.Builder) {
        parameter.serializeAnnotations(proto, protocol.parameterAnnotation)
    }

    override fun serializeTypeAnnotations(annotations: List<FirAnnotation>, proto: ProtoBuf.Type.Builder) {
        annotations.serializeAnnotations(proto, protocol.typeAnnotation)
    }

    override fun serializeTypeParameter(typeParameter: FirTypeParameter, proto: ProtoBuf.TypeParameter.Builder) {
        typeParameter.serializeAnnotations(proto, protocol.typeParameterAnnotation)
    }

    @Suppress("Reformat")
    private fun <
        MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>,
        BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
    > FirAnnotationContainer.serializeAnnotations(
        proto: GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
        extension: GeneratedMessageLite.GeneratedExtension<MessageType, List<ProtoBuf.Annotation>>?
    ) {
        if (extension == null) return
        this.nonSourceAnnotations(session).serializeAnnotations(proto, extension)
    }

    @Suppress("Reformat")
    private fun <
        MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>,
        BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
    > List<FirAnnotation>.serializeAnnotations(
        proto: GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
        extension: GeneratedMessageLite.GeneratedExtension<MessageType, List<ProtoBuf.Annotation>>?,
    ) {
        if (extension == null) return
        for (annotation in this) {
            proto.addExtensionOrNull(extension, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    @Suppress("Reformat")
    private fun <
        MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>,
        BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
        Type
    > GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>.addExtensionOrNull(
        extension: GeneratedMessageLite.GeneratedExtension<MessageType, List<Type>>,
        value: Type?
    ) {
        if (value != null) {
            addExtension(extension, value)
        }
    }
}
