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

import java.io.File
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.kotlin.serialization.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import java.io.ByteArrayOutputStream
import org.jetbrains.kotlin.builtins.BuiltInsSerializationUtil
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.recursePostOrder
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.resolve.jvm.JvmAnalyzerFacade
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
import org.jetbrains.kotlin.analyzer.ModuleContent
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.JVMConfigurationKeys
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId

public class BuiltInsSerializer(private val dependOnOldBuiltIns: Boolean) {
    private var totalSize = 0
    private var totalFiles = 0

    public fun serialize(
            destDir: File,
            srcDirs: Collection<File>,
            extraClassPath: Collection<File>,
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

    private fun serialize(disposable: Disposable, destDir: File, srcDirs: Collection<File>, extraClassPath: Collection<File>) {
        val configuration = CompilerConfiguration()
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        val sourceRoots = srcDirs map { it.path }
        configuration.put(CommonConfigurationKeys.SOURCE_ROOTS_KEY, sourceRoots)

        for (path in extraClassPath) {
            configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, path)
        }

        val environment = JetCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        val files = environment.getSourceFiles()

        val builtInModule = BuiltinsSourcesModule()
        val resolver = JvmAnalyzerFacade.setupResolverForProject(
                GlobalContext(), environment.getProject(), listOf(builtInModule),
                { ModuleContent(files, GlobalSearchScope.EMPTY_SCOPE) },
                platformParameters = JvmPlatformParameters { throw IllegalStateException() }
        )

        val moduleDescriptor = resolver.descriptorForModule(builtInModule)

        // We don't use FileUtil because it spawns JNA initialization, which fails because we don't have (and don't want to have) its
        // native libraries in the compiler jar (libjnidispatch.so / jnidispatch.dll / ...)
        destDir.recursePostOrder { it.delete() }

        if (!destDir.mkdirs()) {
            System.err.println("Could not make directories: " + destDir)
        }

        files.map { it.getPackageFqName() }.toSet().forEach {
            fqName ->
            serializePackage(moduleDescriptor, fqName, destDir)
        }
    }

    private fun serializePackage(module: ModuleDescriptor, fqName: FqName, destDir: File) {
        val packageView = module.getPackage(fqName) ?: error("No package resolved in $module")

        // TODO: perform some kind of validation? At the moment not possible because DescriptorValidator is in compiler-tests
        // DescriptorValidator.validate(packageView)

        val serializer = DescriptorSerializer.createTopLevel(BuiltInsSerializerExtension)

        val classifierDescriptors = DescriptorSerializer.sort(packageView.getMemberScope().getDescriptors(DescriptorKindFilter.CLASSIFIERS))

        serializeClasses(classifierDescriptors, serializer) {
            (classDescriptor, classProto) ->
            val stream = ByteArrayOutputStream()
            classProto.writeTo(stream)
            write(destDir, getFileName(classDescriptor), stream)
        }

        val packageStream = ByteArrayOutputStream()
        val fragments = module.getPackageFragmentProvider().getPackageFragments(fqName)
        val packageProto = serializer.packageProto(fragments).build() ?: error("Package fragments not serialized: $fragments")
        packageProto.writeTo(packageStream)
        write(destDir, BuiltInsSerializationUtil.getPackageFilePath(fqName), packageStream)

        val nameStream = ByteArrayOutputStream()
        NameSerializationUtil.serializeStringTable(nameStream, serializer.getStringTable())
        write(destDir, BuiltInsSerializationUtil.getStringTableFilePath(fqName), nameStream)
    }

    private fun write(destDir: File, fileName: String, stream: ByteArrayOutputStream) {
        totalSize += stream.size()
        totalFiles++
        val file = File(destDir, fileName)
        file.getParentFile()?.mkdirs()
        file.writeBytes(stream.toByteArray())
    }

    private fun serializeClass(
            classDescriptor: ClassDescriptor,
            serializer: DescriptorSerializer,
            writeClass: (ClassDescriptor, ProtoBuf.Class) -> Unit
    ) {
        val classProto = serializer.classProto(classDescriptor).build() ?: error("Class not serialized: $classDescriptor")
        writeClass(classDescriptor, classProto)

        serializeClasses(classDescriptor.getUnsubstitutedInnerClassesScope().getDescriptors(), serializer, writeClass)
    }

    private fun serializeClasses(
            descriptors: Collection<DeclarationDescriptor>,
            serializer: DescriptorSerializer,
            writeClass: (ClassDescriptor, ProtoBuf.Class) -> Unit
    ) {
        for (descriptor in descriptors) {
            if (descriptor is ClassDescriptor) {
                serializeClass(descriptor, serializer, writeClass)
            }
        }
    }

    private fun getFileName(classDescriptor: ClassDescriptor): String {
        return BuiltInsSerializationUtil.getClassMetadataPath(classDescriptor.classId)
    }
}
