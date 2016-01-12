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
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleContent
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.builtins.BuiltInsSerializedResourcePaths
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
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

    private inner class BuiltinsSourcesModule : ModuleInfo {
        override val name = Name.special("<module for resolving builtin source files>")
        override fun dependencies() = listOf(this)
        override fun dependencyOnBuiltins(): ModuleInfo.DependencyOnBuiltins =
                if (dependOnOldBuiltIns) ModuleInfo.DependenciesOnBuiltins.LAST else ModuleInfo.DependenciesOnBuiltins.NONE
    }

    private fun serialize(disposable: Disposable, destDir: File, srcDirs: List<File>, extraClassPath: List<File>) {
        val configuration = CompilerConfiguration()
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        configuration.addKotlinSourceRoots(srcDirs.map { it.path })
        configuration.addJvmClasspathRoots(extraClassPath)

        val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        val files = environment.getSourceFiles()

        val builtInModule = BuiltinsSourcesModule()
        val resolver = JvmAnalyzerFacade.setupResolverForProject(
                "builtIns source",
                ProjectContext(environment.project), listOf(builtInModule),
                { ModuleContent(files, GlobalSearchScope.EMPTY_SCOPE) },
                JvmPlatformParameters { throw IllegalStateException() },
                CompilerEnvironment,
                packagePartProviderFactory = { module, content -> JvmPackagePartProvider(environment) }
        )

        val moduleDescriptor = resolver.descriptorForModule(builtInModule)

        destDir.deleteRecursively()

        if (!destDir.mkdirs()) {
            System.err.println("Could not make directories: " + destDir)
        }

        files.map { it.packageFqName }.toSet().forEach {
            fqName ->
            PackageSerializer(moduleDescriptor.getPackage(fqName), destDir, { bytesWritten ->
                totalSize += bytesWritten
                totalFiles++
            }).run()
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
            proto.addClass(classProto)

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
            write(BuiltInsSerializedResourcePaths.getBuiltInsFilePath(fqName), stream)
        }

        private fun write(fileName: String, stream: ByteArrayOutputStream) {
            onFileWrite(stream.size())
            val file = File(destDir, fileName)
            file.parentFile.mkdirs()
            file.writeBytes(stream.toByteArray())
        }
    }
}
