/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.builtins

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.metadata.AbstractFirMetadataSerializer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
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
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

internal class FirBuiltInsSerializer(
    configuration: CompilerConfiguration,
    environment: KotlinCoreEnvironment,
) : AbstractFirMetadataSerializer(configuration, environment) {
    private var totalSize: Int = 0
    private var totalFiles: Int = 0

    override fun serialize(analysisResult: List<ModuleCompilerAnalyzedOutput>, destDir: File): OutputInfo? {
        val (session, scopeSession, firFiles) = analysisResult.single()
        destDir.deleteRecursively()
        if (!destDir.mkdirs()) {
            error("Could not make directories: $destDir")
        }

        class PackageContent {
            val classes: MutableList<FirRegularClass> = mutableListOf()
            val members: MutableList<FirMemberDeclaration> = mutableListOf()
        }

        val contentPerPackage: MutableMap<FqName, PackageContent> = mutableMapOf()

        for (firFile in firFiles) {
            val packageFqName = firFile.packageFqName
            for (declaration in firFile.declarations) {
                when (declaration) {
                    is FirCallableDeclaration,
                    is FirTypeAlias -> {
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

        for ((packageFqName, content) in contentPerPackage) {
            val destFile = File(destDir, BuiltInSerializerProtocol.getBuiltInsFilePath(packageFqName))
            val serializer = PackageSerializer(
                packageFqName, content.classes, content.members,
                destFile, session, scopeSession
            )
            serializer.serialize()
        }

        return OutputInfo(totalSize, totalFiles)
    }

    private inner class PackageSerializer(
        val packageFqName: FqName,
        val classes: List<FirRegularClass>,
        val members: List<FirMemberDeclaration>,
        val destFile: File,
        val session: FirSession,
        val scopeSession: ScopeSession
    ) {
        private val proto = ProtoBuf.PackageFragment.newBuilder()
        private val extension = FirBuiltInsSerializerExtension(session, scopeSession)
        val typeApproximator = TypeApproximatorForMetadataSerializer(session)
        private val rootSerializer = FirElementSerializer.createTopLevel(
            session, scopeSession, extension, typeApproximator,
            session.languageVersionSettings
        )

        fun serialize() {
            serializeClasses(classes, rootSerializer)
            serializeMembers()
            serializeStringTable()
            serializeBuiltInsFile()
        }

        private fun serializeClasses(classes: List<FirRegularClass>, parentSerializer: FirElementSerializer) {
            for (klass in classes.sortedWith(FirMemberDeclarationComparator)) {
                val classSerializer = FirElementSerializer.create(
                    session, scopeSession, klass, extension,
                    parentSerializer, typeApproximator, session.languageVersionSettings
                )

                @OptIn(SymbolInternals::class)
                val nestedClasses = classSerializer.computeNestedClassifiersForClass(klass.symbol).map { it.fir as FirRegularClass }
                if (nestedClasses.isNotEmpty()) {
                    serializeClasses(nestedClasses, classSerializer)
                }

                val classProto = classSerializer.classProto(klass)
                proto.addClass_(classProto.build())
            }
        }

        private fun serializeMembers() {
            @OptIn(FirElementSerializer.SensitiveApi::class)
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

        private fun serializeBuiltInsFile() {
            val stream = ByteArrayOutputStream()
            with(DataOutputStream(stream)) {
                val version = extension.metadataVersion.toArray()
                writeInt(version.size)
                version.forEach { writeInt(it) }
            }
            proto.build().writeTo(stream)
            write(stream)
        }

        private fun write(stream: ByteArrayOutputStream) {
            totalSize += stream.size()
            totalFiles++
            assert(!destFile.isDirectory) { "Cannot write because output destination is a directory: $destFile" }
            destFile.parentFile.mkdirs()
            destFile.writeBytes(stream.toByteArray())
        }
    }

    private class FirBuiltInsSerializerExtension(
        override val session: FirSession,
        override val scopeSession: ScopeSession
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