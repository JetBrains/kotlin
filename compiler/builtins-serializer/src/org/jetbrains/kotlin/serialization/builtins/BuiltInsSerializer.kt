/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization.builtins

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.common.DefaultAnalyzerFacade
import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

class BuiltInsSerializer(private val dependOnOldBuiltIns: Boolean) {
    private var totalSize = 0
    private var totalFiles = 0

    fun serialize(
            destDir: File,
            srcDirs: List<File>,
            extraClassPath: List<File>,
            onComplete: (totalSize: Int, totalFiles: Int) -> Unit
    ) {
        val rootDisposable = Disposer.newDisposable()
        try {
            serialize(rootDisposable, destDir, srcDirs, extraClassPath)
            onComplete(totalSize, totalFiles)
        }
        finally {
            Disposer.dispose(rootDisposable)
        }
    }

    private fun serialize(disposable: Disposable, destDir: File, srcDirs: List<File>, extraClassPath: List<File>) {
        val configuration = CompilerConfiguration().apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, createMessageCollector())

            addKotlinSourceRoots(srcDirs.map { it.path })
            addJvmClasspathRoots(extraClassPath)
        }

        val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        val files = environment.getSourceFiles()

        val analyzer = AnalyzerWithCompilerReport(MessageCollector.NONE)
        analyzer.analyzeAndReport(files, object : AnalyzerWithCompilerReport.Analyzer {
            override fun analyze(): AnalysisResult = DefaultAnalyzerFacade.analyzeFiles(
                    files, Name.special("<module for resolving builtin source files>"), dependOnOldBuiltIns
            )
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

    private fun createMessageCollector() = object : GroupingMessageCollector(
            PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, /* verbose = */ false)
    ) {
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
            // Only report diagnostics without a particular location because there's plenty of errors in built-in sources
            // (functions without bodies, incorrect combination of modifiers, etc.)
            if (location.path == null) {
                super.report(severity, message, location)
            }
        }
    }

    private class PackageSerializer(
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
            write(BuiltInSerializerProtocol.getBuiltInsFilePath(fqName), stream)
        }

        private fun write(fileName: String, stream: ByteArrayOutputStream) {
            onFileWrite(stream.size())
            val file = File(destDir, fileName)
            file.parentFile.mkdirs()
            file.writeBytes(stream.toByteArray())
        }
    }
}
