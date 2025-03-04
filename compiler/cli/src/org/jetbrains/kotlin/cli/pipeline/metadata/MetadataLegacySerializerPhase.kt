/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.metadata

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.common.metadataDestinationDirectory
import org.jetbrains.kotlin.cli.metadata.AbstractMetadataSerializer.OutputInfo
import org.jetbrains.kotlin.cli.metadata.getClassFilePath
import org.jetbrains.kotlin.cli.metadata.getPackageFilePath
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
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
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.metadata.jvm.deserialization.serializeToByteArray
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.SerializableStringTable
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

object MetadataLegacySerializerPhase : MetadataLegacySerializerPhaseBase(name = "MetadataLegacySerializerPhase") {
    override fun serialize(
        analysisResult: List<ModuleCompilerAnalyzedOutput>,
        destDir: File,
        metadataVersion: BuiltInsBinaryVersion,
    ): OutputInfo {
        val (session, scopeSession, firFiles) = analysisResult.single()
        val contentPerPackage = collectPackagesContent(firFiles)

        val packageTable = mutableMapOf<FqName, PackageParts>()
        val counters = Counters()

        for ((packageFqName, content) in contentPerPackage) {
            val (classes, membersPerFile) = content
            for (klass in classes) {
                val destFile = File(destDir, getClassFilePath(klass.classId))
                PackageSerializer(
                    packageFqName, classes = listOf(klass), members = emptyList(),
                    destFile, session, scopeSession, metadataVersion, counters
                ).serialize()
            }
            for ((file, members) in membersPerFile) {
                val destFile = File(destDir, getPackageFilePath(packageFqName, file.name))
                PackageSerializer(
                    packageFqName, classes = emptyList(), members = members,
                    destFile, session, scopeSession, metadataVersion, counters
                ).serialize()

                packageTable.getOrPut(packageFqName) {
                    PackageParts(packageFqName.asString())
                }.addMetadataPart(destFile.nameWithoutExtension)
            }
        }
        val kotlinModuleFile = File(destDir, JvmCodegenUtil.getMappingFileName(JvmCodegenUtil.prepareModuleName(session.moduleData.name)))
        val packageTableBytes = JvmModuleProtoBuf.Module.newBuilder().apply {
            for (table in packageTable.values) {
                table.addTo(this)
            }
        }.build().serializeToByteArray(MetadataVersion.INSTANCE, 0)

        kotlinModuleFile.parentFile.mkdirs()
        kotlinModuleFile.writeBytes(packageTableBytes)
        return OutputInfo(counters.totalSize, counters.totalFiles)
    }

}

object MetadataBuiltinsSerializerPhase : MetadataLegacySerializerPhaseBase(name = "MetadataBuiltinsSerializerPhase") {
    override fun serialize(
        analysisResult: List<ModuleCompilerAnalyzedOutput>,
        destDir: File,
        metadataVersion: BuiltInsBinaryVersion,
    ): OutputInfo? {
        val (session, scopeSession, firFiles) = analysisResult.single()
        destDir.deleteRecursively()
        if (!destDir.mkdirs()) {
            error("Could not make directories: $destDir")
        }

        val contentPerPackage = collectPackagesContent(firFiles)
        @OptIn(SymbolInternals::class)
        contentPerPackage.getOrPut(StandardNames.BUILT_INS_PACKAGE_FQ_NAME) { PackageContent() }.classes +=
            FirCloneableSymbolProvider(session, session.moduleData, session.kotlinScopeProvider)
                .getClassLikeSymbolByClassId(StandardClassIds.Cloneable)!!.fir as FirRegularClass

        val counters = Counters()
        for ((packageFqName, content) in contentPerPackage) {
            val destFile = File(destDir, BuiltInSerializerProtocol.getBuiltInsFilePath(packageFqName))
            val serializer = PackageSerializer(
                packageFqName, content.classes, content.membersPerFile.values.flatten(),
                destFile, session, scopeSession, BuiltInsBinaryVersion.INSTANCE, counters
            )
            serializer.serialize()
        }

        return OutputInfo(counters.totalSize, counters.totalFiles)
    }
}

@OptIn(DirectDeclarationsAccess::class)
abstract class MetadataLegacySerializerPhaseBase(
    name: String
) : PipelinePhase<MetadataFrontendPipelineArtifact, MetadataSerializationArtifact>(
    name = name,
    preActions = setOf(PerformanceNotifications.BackendStarted),
    postActions = setOf(PerformanceNotifications.BackendFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    final override fun executePhase(input: MetadataFrontendPipelineArtifact): MetadataSerializationArtifact {
        val (firResult, configuration, _, _) = input
        val metadataVersion = input.metadataVersion
        val destDir = configuration.metadataDestinationDirectory!!
        val outputInfo = serialize(firResult.outputs, destDir, metadataVersion)
        return MetadataSerializationArtifact(
            outputInfo,
            configuration,
            destDir.canonicalPath
        )
    }

    protected abstract fun serialize(
        analysisResult: List<ModuleCompilerAnalyzedOutput>,
        destDir: File,
        metadataVersion: BuiltInsBinaryVersion,
    ): OutputInfo?

    protected data class PackageContent(
        val classes: MutableList<FirRegularClass> = mutableListOf(),
        val membersPerFile: MutableMap<FirFile, MutableList<FirMemberDeclaration>> = mutableMapOf()
    )

    protected fun collectPackagesContent(firFiles: List<FirFile>): MutableMap<FqName, PackageContent> {
        val contentPerPackage: MutableMap<FqName, PackageContent> = mutableMapOf()

        for (firFile in firFiles) {
            val packageFqName = firFile.packageFqName
            for (declaration in firFile.declarations) {
                val content = contentPerPackage.getOrPut(packageFqName) { PackageContent() }
                when (declaration) {
                    is FirCallableDeclaration,
                    is FirTypeAlias -> {
                        content.membersPerFile.getOrPut(firFile) { mutableListOf() } += declaration
                    }
                    is FirRegularClass -> {
                        content.classes += declaration
                    }
                    else -> error("Unexpected declaration: ${declaration.render()}")
                }
            }
        }

        return contentPerPackage
    }

    protected class PackageSerializer(
        val packageFqName: FqName,
        val classes: List<FirRegularClass>,
        val members: List<FirMemberDeclaration>,
        val destFile: File,
        val session: FirSession,
        val scopeSession: ScopeSession,
        metadataVersion: BinaryVersion,
        val counters: Counters
    ) {
        private val extension = FirLegacySerializerExtension(session, scopeSession, metadataVersion)
        private val proto = ProtoBuf.PackageFragment.newBuilder()
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

                val file = session.firProvider.getFirClassifierContainerFileIfAny(klass.symbol)
                val classProto = classSerializer.classProto(klass, file)
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
            val (strings, qualifiedNames) = (extension.stringTable as? SerializableStringTable)?.buildProto() ?: return
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
            counters.totalSize += stream.size()
            counters.totalFiles++
            assert(!destFile.isDirectory) { "Cannot write because output destination is a directory: $destFile" }
            destFile.parentFile.mkdirs()
            destFile.writeBytes(stream.toByteArray())
        }
    }

    protected class Counters {
        var totalSize: Int = 0
        var totalFiles: Int = 0
    }

    class FirLegacySerializerExtension(
        override val session: FirSession,
        override val scopeSession: ScopeSession,
        override val metadataVersion: BinaryVersion
    ) : FirSerializerExtensionBase(BuiltInSerializerProtocol) {
        override val constValueProvider: ConstValueProvider?
            get() = null
        override val additionalMetadataProvider: FirAdditionalMetadataProvider?
            get() = null

        override fun shouldUseTypeTable(): Boolean = true
    }
}
