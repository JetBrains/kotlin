/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.metadata

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.metadata.jvm.deserialization.serializeToByteArray
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.deserialization.DOT_METADATA_FILE_EXTENSION
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Produces legacy metadata artifact using K1 compiler
 */
open class K1LegacyMetadataSerializer(
    configuration: CompilerConfiguration,
    environment: KotlinCoreEnvironment,
    private val dependOnOldBuiltIns: Boolean,
    definedMetadataVersion: BuiltInsBinaryVersion? = null
) : AbstractMetadataSerializer<CommonAnalysisResult>(configuration, environment, definedMetadataVersion) {
    protected var totalSize = 0
    protected var totalFiles = 0

    override fun analyze(): CommonAnalysisResult? {
        return runCommonAnalysisForSerialization(environment, dependOnOldBuiltIns, dependencyContainerFactory = { null })
    }

    override fun serialize(analysisResult: CommonAnalysisResult, destDir: File): OutputInfo {
        val languageVersionSettings = environment.configuration.languageVersionSettings
        val files = environment.getSourceFiles()
        val project = environment.project
        val (module, bindingContext) = analysisResult

        val packageTable = hashMapOf<FqName, PackageParts>()

        for (file in files) {
            val packageFqName = file.packageFqName
            val members = arrayListOf<DeclarationDescriptor>()
            for (declaration in file.declarations) {
                declaration.accept(object : KtVisitorVoid() {
                    override fun visitNamedFunction(function: KtNamedFunction) {
                        members.add(
                            bindingContext.get(BindingContext.FUNCTION, function)
                                ?: error("No descriptor found for function ${function.fqName}")
                        )
                    }

                    override fun visitProperty(property: KtProperty) {
                        members.add(
                            bindingContext.get(BindingContext.VARIABLE, property)
                                ?: error("No descriptor found for property ${property.fqName}")
                        )
                    }

                    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
                        members.add(
                            bindingContext.get(BindingContext.TYPE_ALIAS, typeAlias)
                                ?: error("No descriptor found for type alias ${typeAlias.fqName}")
                        )
                    }

                    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                        val classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject)
                            ?: error("No descriptor found for class ${classOrObject.fqName}")
                        val destFile = File(destDir, getClassFilePath(ClassId(packageFqName, classDescriptor.name)))
                        PackageSerializer(
                            listOf(classDescriptor), emptyList(), packageFqName, destFile,
                            languageVersionSettings, project,
                        ).run()
                    }
                })
            }

            if (members.isNotEmpty()) {
                val destFile = File(destDir, getPackageFilePath(packageFqName, file.name))
                PackageSerializer(emptyList(), members, packageFqName, destFile, languageVersionSettings, project).run()

                packageTable.getOrPut(packageFqName) {
                    PackageParts(packageFqName.asString())
                }.addMetadataPart(destFile.nameWithoutExtension)
            }
        }

        val kotlinModuleFile = File(destDir, JvmCodegenUtil.getMappingFileName(JvmCodegenUtil.getModuleName(module)))
        val packageTableBytes = JvmModuleProtoBuf.Module.newBuilder().apply {
            for (table in packageTable.values) {
                table.addTo(this)
            }
        }.build().serializeToByteArray(MetadataVersion.INSTANCE, 0) // TODO: use another version here, not JVM
        // TODO: also, use CommonConfigurationKeys.METADATA_VERSION if needed

        kotlinModuleFile.parentFile.mkdirs()
        kotlinModuleFile.writeBytes(packageTableBytes)
        return OutputInfo(totalSize, totalFiles)
    }

    protected open fun createSerializerExtension(): KotlinSerializerExtensionBase = MetadataSerializerExtension(metadataVersion)

    protected inner class PackageSerializer(
        private val classes: Collection<DeclarationDescriptor>,
        private val members: Collection<DeclarationDescriptor>,
        private val packageFqName: FqName,
        private val destFile: File,
        private val languageVersionSettings: LanguageVersionSettings,
        private val project: Project? = null
    ) {
        private val proto = ProtoBuf.PackageFragment.newBuilder()
        private val extension = createSerializerExtension()

        fun run() {
            val serializer = DescriptorSerializer.createTopLevel(extension, languageVersionSettings, project)
            serializeClasses(classes, serializer, project)
            serializeMembers(members, serializer)
            serializeStringTable()
            serializeBuiltInsFile()
        }

        private fun serializeClasses(classes: Collection<DeclarationDescriptor>, parentSerializer: DescriptorSerializer, project: Project?) {
            for (descriptor in DescriptorSerializer.sort(classes)) {
                if (descriptor !is ClassDescriptor || descriptor.kind == ClassKind.ENUM_ENTRY) continue

                val serializer = DescriptorSerializer.create(descriptor, extension, parentSerializer, languageVersionSettings, project)
                serializeClasses(
                    descriptor.unsubstitutedInnerClassesScope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS),
                    serializer,
                    project
                )

                proto.addClass_(serializer.classProto(descriptor).build())
            }
        }

        private fun serializeMembers(members: Collection<DeclarationDescriptor>, serializer: DescriptorSerializer) {
            proto.`package` = serializer.packagePartProto(packageFqName, members).build()
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
}

internal fun getClassFilePath(classId: ClassId): String =
    classId.asSingleFqName().asString().replace('.', '/') + DOT_METADATA_FILE_EXTENSION

internal fun getPackageFilePath(packageFqName: FqName, fileName: String): String =
    packageFqName.asString().replace('.', '/') + "/" +
            PackagePartClassUtils.getFilePartShortName(fileName) + DOT_METADATA_FILE_EXTENSION
