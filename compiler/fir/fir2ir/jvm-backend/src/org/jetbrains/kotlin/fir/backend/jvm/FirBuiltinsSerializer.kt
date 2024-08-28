/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.metadata.BuiltinsSerializer
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCloneableSymbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.serialization.FirAdditionalMetadataProvider
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.serialization.FirSerializerExtensionBase
import org.jetbrains.kotlin.fir.serialization.TypeApproximatorForMetadataSerializer
import org.jetbrains.kotlin.fir.serialization.constant.ConstValueProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class FirBuiltInsSerializer(val session: FirSession, val scopeSession: ScopeSession): BuiltinsSerializer {

    override fun serialize(filesMetadata: List<MetadataSource.File>): List<Pair<FqName, ByteArray>> {
        class PackageContent {
            val classes: MutableList<FirRegularClass> = mutableListOf()
            val members: MutableList<FirMemberDeclaration> = mutableListOf()
        }

        val contentPerPackage: MutableMap<FqName, PackageContent> = mutableMapOf()

        for (firFile in filesMetadata.map { (it as FirMetadataSource.File).fir }) {
            val packageFqName = firFile.packageFqName
            for (declaration in firFile.declarations) {
                when (declaration) {
                    is FirCallableDeclaration,
                    is FirTypeAlias,
                        -> {
                        val content = contentPerPackage.getOrPut(packageFqName) { PackageContent() }
                        content.members += declaration
                    }
                    is FirRegularClass -> {
                        val content = contentPerPackage.getOrPut(packageFqName) { PackageContent() }
                        content.classes += declaration
                    }
                    else -> error("Unexpected declaration: ${declaration.render()}")
                }
            }
        }

        @OptIn(SymbolInternals::class)
        contentPerPackage.getOrPut(StandardNames.BUILT_INS_PACKAGE_FQ_NAME) { PackageContent() }.classes +=
            FirCloneableSymbolProvider(session, session.moduleData, session.kotlinScopeProvider)
                .getClassLikeSymbolByClassId(StandardClassIds.Cloneable)!!.fir as FirRegularClass

        return contentPerPackage.map { packageWithContent ->
            val (packageFqName, content) = packageWithContent
            val serializer = PackageSerializer(packageFqName, content.classes, content.members, session, scopeSession)
            Pair(packageFqName, serializer.serialize())
        }.toList()
    }

    override fun serializeEmptyPackage(fqName: FqName): ByteArray {
        return PackageSerializer(fqName, emptyList(), emptyList(), session, scopeSession).serialize()
    }

    private inner class PackageSerializer(
        val packageFqName: FqName,
        val classes: List<FirRegularClass>,
        val members: List<FirMemberDeclaration>,
        val session: FirSession,
        val scopeSession: ScopeSession,
    ) {
        private val proto = ProtoBuf.PackageFragment.newBuilder()
        private val extension = FirBuiltInsSerializerExtension(session, scopeSession)
        val typeApproximator = TypeApproximatorForMetadataSerializer(session)
        private val rootSerializer = FirElementSerializer.createTopLevel(
            session, scopeSession, extension, typeApproximator,
            session.languageVersionSettings
        )

        fun serialize(): ByteArray {
            serializeClasses(classes, rootSerializer)
            serializeMembers()
            serializeStringTable()
            return serializeToByteArray()
        }

        private fun serializeClasses(classes: List<FirRegularClass>, parentSerializer: FirElementSerializer) {
            for (klass in classes.sortedWith(FirMemberDeclarationComparator)) {
                @OptIn(SymbolInternals::class)
                val nestedClasses = parentSerializer.computeNestedClassifiersForClass(klass.symbol).map { it.fir as FirRegularClass }
                if (nestedClasses.isNotEmpty()) {
                    val nestedSerializer = FirElementSerializer.create(
                        session, scopeSession, klass, extension,
                        parentSerializer, typeApproximator, session.languageVersionSettings
                    )
                    serializeClasses(nestedClasses, nestedSerializer)
                }

                val classProto = parentSerializer.classProto(klass)
                proto.addClass_(classProto.build())
            }
        }

        @OptIn(FirElementSerializer.SensitiveApi::class)
        private fun serializeMembers() {
            val packagePartProto = rootSerializer.packagePartProto(
                packageFqName,
                members.sortedWith(FirMemberDeclarationComparator),
                actualizedExpectDeclarations = null
            )
            proto.`package` = packagePartProto.build()
        }

        private fun serializeStringTable() {
            val (strings, qualifiedNames) = extension.stringTable.buildProto()
            proto.strings = strings
            proto.qualifiedNames = qualifiedNames
        }

        private fun serializeToByteArray(): ByteArray {
            val stream = ByteArrayOutputStream()
            with(DataOutputStream(stream)) {
                val version = extension.metadataVersion.toArray()
                writeInt(version.size)
                version.forEach { writeInt(it) }
            }
            proto.build().writeTo(stream)
            return stream.toByteArray()
        }
    }

    private class FirBuiltInsSerializerExtension(
        override val session: FirSession,
        override val scopeSession: ScopeSession,
    ) : FirSerializerExtensionBase(BuiltInSerializerProtocol) {
        override val metadataVersion: BinaryVersion
            get() = BuiltInsBinaryVersion.INSTANCE
        override val constValueProvider: ConstValueProvider?
            get() = null
        override val additionalMetadataProvider: FirAdditionalMetadataProvider?
            get() = null

        override fun shouldUseTypeTable(): Boolean {
            return true
        }
    }
}