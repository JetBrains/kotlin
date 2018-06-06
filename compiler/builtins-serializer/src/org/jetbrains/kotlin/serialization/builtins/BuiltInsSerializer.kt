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

package org.jetbrains.kotlin.serialization.builtins

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.builtins.JvmBuiltInClassDescriptorFactory
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.metadata.MetadataSerializer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File

class BuiltInsSerializer(dependOnOldBuiltIns: Boolean) : MetadataSerializer(dependOnOldBuiltIns) {
    fun serialize(
            destDir: File,
            srcDirs: List<File>,
            extraClassPath: List<File>,
            onComplete: (totalSize: Int, totalFiles: Int) -> Unit
    ) {
        val rootDisposable = Disposer.newDisposable()
        val messageCollector = createMessageCollector()
        try {
            val configuration = CompilerConfiguration().apply {
                put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)

                addKotlinSourceRoots(srcDirs.map { it.path })
                addJvmClasspathRoots(extraClassPath)

                put(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY, destDir)
                put(CommonConfigurationKeys.MODULE_NAME, "module for built-ins serialization")
            }

            val environment = KotlinCoreEnvironment.createForTests(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            serialize(environment)

            onComplete(totalSize, totalFiles)
        }
        finally {
            messageCollector.flush()
            Disposer.dispose(rootDisposable)
        }
    }

    private fun createMessageCollector() = object : GroupingMessageCollector(
            PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false),
            false
    ) {
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
            // Only report diagnostics without a particular location because there's plenty of errors in built-in sources
            // (functions without bodies, incorrect combination of modifiers, etc.)
            if (location == null) {
                super.report(severity, message, location)
            }
        }
    }

    override fun performSerialization(files: Collection<KtFile>, bindingContext: BindingContext, module: ModuleDescriptor, destDir: File) {
        destDir.deleteRecursively()
        if (!destDir.mkdirs()) {
            throw AssertionError("Could not make directories: " + destDir)
        }

        files.map { it.packageFqName }.toSet().forEach {
            fqName ->
            val packageView = module.getPackage(fqName)
            PackageSerializer(
                    packageView.memberScope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) + createCloneable(module),
                    packageView.fragments.flatMap { fragment -> DescriptorUtils.getAllDescriptors(fragment.getMemberScope()) },
                    packageView.fqName,
                    File(destDir, BuiltInSerializerProtocol.getBuiltInsFilePath(packageView.fqName))
            ).run()
        }
    }

    override fun createSerializerExtension(): KotlinSerializerExtensionBase = BuiltInsSerializerExtension()

    // Serialize metadata for kotlin.Cloneable manually for compatibility with kotlin-reflect 1.0 which expects this metadata to be there.
    // Since Kotlin 1.1, we always discard this class during deserialization (see ClassDeserializer.kt).
    private fun createCloneable(module: ModuleDescriptor): ClassDescriptor {
        val factory = JvmBuiltInClassDescriptorFactory(LockBasedStorageManager.NO_LOCKS, module) {
            EmptyPackageFragmentDescriptor(module, KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME)
        }
        return factory.createClass(ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.cloneable.toSafe()))
               ?: error("Could not create kotlin.Cloneable in $module")
    }
}
