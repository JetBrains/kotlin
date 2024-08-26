/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.metadata

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.comparators.FirMemberDeclarationComparator
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.serialization.FirAdditionalMetadataProvider
import org.jetbrains.kotlin.fir.serialization.FirElementSerializer
import org.jetbrains.kotlin.fir.serialization.FirSerializerExtensionBase
import org.jetbrains.kotlin.fir.serialization.TypeApproximatorForMetadataSerializer
import org.jetbrains.kotlin.fir.serialization.constant.ConstValueProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.metadata.jvm.deserialization.serializeToByteArray
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.SerializableStringTable
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

internal open class FirLegacyMetadataSerializer(
    configuration: CompilerConfiguration,
    environment: KotlinCoreEnvironment,
) : AbstractFirMetadataSerializer(configuration, environment) {
    protected var totalSize: Int = 0
    protected var totalFiles: Int = 0

    override fun serialize(analysisResult: List<ModuleCompilerAnalyzedOutput>, destDir: File): OutputInfo? {
        val (session, scopeSession, firFiles) = analysisResult.single()
        val contentPerPackage = collectPackagesContent(firFiles)

        val packageTable = mutableMapOf<FqName, PackageParts>()

        for ((packageFqName, content) in contentPerPackage) {
            val (classes, membersPerFile) = content
            for (klass in classes) {
                val destFile = File(destDir, getClassFilePath(klass.classId))
                PackageSerializer(
                    packageFqName, classes = listOf(klass), members = emptyList(),
                    destFile, session, scopeSession, metadataVersion
                ).serialize()
            }
            for ((file, members) in membersPerFile) {
                val destFile = File(destDir, getPackageFilePath(packageFqName, file.name))
                PackageSerializer(
                    packageFqName, classes = emptyList(), members = members,
                    destFile, session, scopeSession, metadataVersion
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
        }.build().serializeToByteArray(JvmMetadataVersion.INSTANCE, 0)

        kotlinModuleFile.parentFile.mkdirs()
        kotlinModuleFile.writeBytes(packageTableBytes)
        return OutputInfo(totalSize, totalFiles)
    }

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

    protected inner class PackageSerializer(
        val packageFqName: FqName,
        val classes: List<FirRegularClass>,
        val members: List<FirMemberDeclaration>,
        val destFile: File,
        val session: FirSession,
        val scopeSession: ScopeSession,
        metadataVersion: BinaryVersion,
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
            totalSize += stream.size()
            totalFiles++
            assert(!destFile.isDirectory) { "Cannot write because output destination is a directory: $destFile" }
            destFile.parentFile.mkdirs()
            destFile.writeBytes(stream.toByteArray())
        }
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