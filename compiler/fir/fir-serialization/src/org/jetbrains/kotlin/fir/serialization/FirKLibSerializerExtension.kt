/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.serialization.constant.ConstValueProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.serialization.deserialization.DYNAMIC_TYPE_DESERIALIZER_ID

class FirKLibSerializerExtension(
    override val session: FirSession,
    override val metadataVersion: BinaryVersion,
    override val constValueProvider: ConstValueProvider?,
    private val allowErrorTypes: Boolean,
    private val exportKDoc: Boolean,
    override val additionalAnnotationsProvider: FirAdditionalMetadataAnnotationsProvider?
) : FirSerializerExtensionBase(KlibMetadataSerializerProtocol) {
    override fun shouldUseTypeTable(): Boolean = true

    override fun serializeFlexibleType(type: ConeFlexibleType, lowerProto: ProtoBuf.Type.Builder, upperProto: ProtoBuf.Type.Builder) {
        lowerProto.flexibleTypeCapabilitiesId = stringTable.getStringIndex(DYNAMIC_TYPE_DESERIALIZER_ID)
    }

    override fun serializeErrorType(type: ConeErrorType, builder: ProtoBuf.Type.Builder) {
        if (!allowErrorTypes) super.serializeErrorType(type, builder)
    }

    override fun serializeClass(
        klass: FirClass,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: FirElementSerializer
    ) {
        klass.setFileId(proto, KlibMetadataProtoBuf.classFile)
        klass.setKDoc(proto, KlibMetadataProtoBuf.classKdoc)
        super.serializeClass(klass, proto, versionRequirementTable, childSerializer)
        childSerializer.typeTable.serialize()?.let { proto.mergeTypeTable(it) }
    }

    override fun serializeConstructor(
        constructor: FirConstructor,
        proto: ProtoBuf.Constructor.Builder,
        childSerializer: FirElementSerializer
    ) {
        constructor.setKDoc(proto, KlibMetadataProtoBuf.constructorKdoc)
        super.serializeConstructor(constructor, proto, childSerializer)
    }

    override fun serializeProperty(
        property: FirProperty,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
        property.setFileId(proto, KlibMetadataProtoBuf.propertyFile)
        property.setKDoc(proto, KlibMetadataProtoBuf.propertyKdoc)
        super.serializeProperty(property, proto, versionRequirementTable, childSerializer)
    }

    override fun serializeFunction(
        function: FirFunction,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
        function.setFileId(proto, KlibMetadataProtoBuf.functionFile)
        function.setKDoc(proto, KlibMetadataProtoBuf.functionKdoc)
        super.serializeFunction(function, proto, versionRequirementTable, childSerializer)
    }

    private val firProvider = session.firProvider

    @Suppress("Reformat")
    private fun <
        MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>,
        BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
    > FirDeclaration.setKDoc(
        proto: GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
        extension: GeneratedMessageLite.GeneratedExtension<MessageType, String>,
    ) {
        if (exportKDoc) {
            findKDocString()?.let { proto.setExtension(extension, it) }
        }
    }

    private fun FirDeclaration.findKDocString(): String? =
        source?.let {
            val kidsRef = Ref<Array<LighterASTNode?>>()
            it.treeStructure.getChildren(it.lighterASTNode, kidsRef)
            kidsRef.get().singleOrNull { it?.tokenType == KtTokens.DOC_COMMENT }?.toString()
        }

    @Suppress("Reformat")
    private fun <
        MessageType : GeneratedMessageLite.ExtendableMessage<MessageType>,
        BuilderType : GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
    > FirDeclaration.setFileId(
        proto: GeneratedMessageLite.ExtendableBuilder<MessageType, BuilderType>,
        extension: GeneratedMessageLite.GeneratedExtension<MessageType, Int>,
    ) {
        declarationFileId(this)?.let { proto.setExtension(extension, it) }
    }

    private fun declarationFileId(declaration: FirDeclaration): Int? {
        val file = when (val symbol = declaration.symbol) {
            is FirCallableSymbol<*> -> firProvider.getFirCallableContainerFile(symbol)
            is FirClassLikeSymbol<*> -> firProvider.getFirClassifierContainerFileIfAny(symbol)
            else -> null
        } ?: return null
        return stringTable.getStringIndex(file.name)
    }
}
