/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.metadata

import org.jetbrains.kotlin.analyzer.common.CommonAnalyzerFacade
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.PackageParts
import org.jetbrains.kotlin.metadata.jvm.deserialization.serializeToByteArray
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment.Companion.DOT_METADATA_FILE_EXTENSION
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

open class MetadataSerializer(
    private val metadataVersion: BuiltInsBinaryVersion,
    private val dependOnOldBuiltIns: Boolean
) {
    protected var totalSize = 0
    protected var totalFiles = 0

    fun serialize(environment: KotlinCoreEnvironment) {
        val configuration = environment.configuration
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val files = environment.getSourceFiles()
        val moduleName = Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>")

        val destDir = configuration.get(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY) ?: run {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify destination via -d")
            return
        }

        val analyzer = AnalyzerWithCompilerReport(messageCollector, configuration.languageVersionSettings)
        analyzer.analyzeAndReport(files) {
            CommonAnalyzerFacade.analyzeFiles(files, moduleName, dependOnOldBuiltIns, configuration.languageVersionSettings) { content ->
                environment.createPackagePartProvider(content.moduleContentScope)
            }
        }

        if (analyzer.hasErrors()) return

        val (bindingContext, moduleDescriptor) = analyzer.analysisResult

        performSerialization(files, bindingContext, moduleDescriptor, destDir)
    }

    protected open fun performSerialization(
        files: Collection<KtFile>, bindingContext: BindingContext, module: ModuleDescriptor, destDir: File
    ) {
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
                        PackageSerializer(listOf(classDescriptor), emptyList(), packageFqName, destFile).run()
                    }
                })
            }

            if (members.isNotEmpty()) {
                val destFile = File(destDir, getPackageFilePath(packageFqName, file.name))
                PackageSerializer(emptyList(), members, packageFqName, destFile).run()

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
        }.build().serializeToByteArray(JvmMetadataVersion.INSTANCE, 0) // TODO: use another version here, not JVM
        // TODO: also, use CommonConfigurationKeys.METADATA_VERSION if needed

        kotlinModuleFile.parentFile.mkdirs()
        kotlinModuleFile.writeBytes(packageTableBytes)
    }

    protected open fun createSerializerExtension(): KotlinSerializerExtensionBase = MetadataSerializerExtension(metadataVersion)

    private fun getPackageFilePath(packageFqName: FqName, fileName: String): String =
        packageFqName.asString().replace('.', '/') + "/" +
                PackagePartClassUtils.getFilePartShortName(fileName) + DOT_METADATA_FILE_EXTENSION

    private fun getClassFilePath(classId: ClassId): String =
        classId.asSingleFqName().asString().replace('.', '/') + DOT_METADATA_FILE_EXTENSION

    protected inner class PackageSerializer(
        private val classes: Collection<DeclarationDescriptor>,
        private val members: Collection<DeclarationDescriptor>,
        private val packageFqName: FqName,
        private val destFile: File
    ) {
        private val proto = ProtoBuf.PackageFragment.newBuilder()
        private val extension = createSerializerExtension()

        fun run() {
            val serializer = DescriptorSerializer.createTopLevel(extension)
            serializeClasses(classes, serializer)
            serializeMembers(members, serializer)
            serializeStringTable()
            serializeBuiltInsFile()
        }

        private fun serializeClasses(classes: Collection<DeclarationDescriptor>, parentSerializer: DescriptorSerializer) {
            for (descriptor in DescriptorSerializer.sort(classes)) {
                if (descriptor !is ClassDescriptor || descriptor.kind == ClassKind.ENUM_ENTRY) continue

                val serializer = DescriptorSerializer.create(descriptor, extension, parentSerializer)
                serializeClasses(
                    descriptor.unsubstitutedInnerClassesScope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS),
                    serializer
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
