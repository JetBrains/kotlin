/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.common.DefaultAnalyzerFacade
import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.serialization.builtins.BuiltInsSerializerExtension
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

open class MetadataSerializer(private val dependOnOldBuiltIns: Boolean) {
    protected var totalSize = 0
    protected var totalFiles = 0

    protected open fun getPackageFilePath(fqName: FqName): String =
            fqName.asString().replace('.', '/') + "/" + (if (fqName.isRoot) "default-package" else fqName.shortName().asString()) + "." + METADATA_FILE_EXTENSION

    fun serialize(environment: KotlinCoreEnvironment) {
        val configuration = environment.configuration
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val files = environment.getSourceFiles()
        val moduleName = Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>")

        val destDir = configuration.get(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY) ?: run {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify destination via -d", CompilerMessageLocation.NO_LOCATION)
            return
        }

        val analyzer = AnalyzerWithCompilerReport(messageCollector)
        analyzer.analyzeAndReport(files, object : AnalyzerWithCompilerReport.Analyzer {
            override fun analyze(): AnalysisResult = DefaultAnalyzerFacade.analyzeFiles(files, moduleName, dependOnOldBuiltIns)
        })

        val moduleDescriptor = analyzer.analysisResult.moduleDescriptor

        destDir.deleteRecursively()

        if (!destDir.mkdirs()) {
            throw AssertionError("Could not make directories: " + destDir)
        }

        files.map { it.packageFqName }.toSet().forEach {
            fqName ->
            PackageSerializer(moduleDescriptor.getPackage(fqName), destDir, { bytesWritten ->
                totalSize += bytesWritten
                totalFiles++
            }).run()
        }
    }

    private inner class PackageSerializer(
            private val packageView: PackageViewDescriptor,
            private val destDir: File,
            private val onFileWrite: (bytesWritten: Int) -> Unit
    ) {
        private val fqName = packageView.fqName
        private val fragments = packageView.fragments
        private val proto = BuiltInsProtoBuf.BuiltIns.newBuilder()
        private val extension = BuiltInsSerializerExtension(fragments)

        fun run() {
            serializeClasses(packageView.memberScope)
            serializePackageFragments(fragments)
            serializeStringTable()
            serializeBuiltInsFile()
        }

        private fun serializeClass(classDescriptor: ClassDescriptor) {
            val classProto = DescriptorSerializer.createTopLevel(extension).classProto(classDescriptor).build()
            proto.addClass_(classProto)

            serializeClasses(classDescriptor.unsubstitutedInnerClassesScope)
        }

        private fun serializeClasses(scope: MemberScope) {
            for (descriptor in DescriptorSerializer.sort(scope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS))) {
                if (descriptor is ClassDescriptor && descriptor.kind != ClassKind.ENUM_ENTRY) {
                    serializeClass(descriptor)
                }
            }
        }

        private fun serializePackageFragments(fragments: List<PackageFragmentDescriptor>) {
            proto.`package` = DescriptorSerializer.createTopLevel(extension).packageProto(fragments).build()
        }

        private fun serializeStringTable() {
            val (strings, qualifiedNames) = extension.stringTable.buildProto()
            proto.strings = strings
            proto.qualifiedNames = qualifiedNames
        }

        private fun serializeBuiltInsFile() {
            val stream = ByteArrayOutputStream()
            with(DataOutputStream(stream)) {
                val version = BuiltInsBinaryVersion.INSTANCE.toArray()
                writeInt(version.size)
                version.forEach { writeInt(it) }
            }
            proto.build().writeTo(stream)
            write(getPackageFilePath(fqName), stream)
        }

        private fun write(fileName: String, stream: ByteArrayOutputStream) {
            onFileWrite(stream.size())
            val file = File(destDir, fileName)
            file.parentFile.mkdirs()
            file.writeBytes(stream.toByteArray())
        }
    }

    companion object {
        private val METADATA_FILE_EXTENSION = ".kotlin_metadata"
    }
}
