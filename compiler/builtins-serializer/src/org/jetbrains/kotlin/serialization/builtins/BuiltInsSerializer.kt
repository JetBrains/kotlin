/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.builtins

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInClassDescriptorFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.metadata.CommonAnalysisResult
import org.jetbrains.kotlin.cli.metadata.MetadataSerializer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File

class BuiltInsSerializer(
    configuration: CompilerConfiguration,
    environment: KotlinCoreEnvironment,
    dependOnOldBuiltIns: Boolean
) : MetadataSerializer(configuration, environment, dependOnOldBuiltIns, BuiltInsBinaryVersion.INSTANCE) {
    companion object {
        fun analyzeAndSerialize(
            destDir: File,
            srcDirs: List<File>,
            extraClassPath: List<File>,
            dependOnOldBuiltIns: Boolean,
            onComplete: (totalSize: Int, totalFiles: Int) -> Unit
        ) {
            val rootDisposable = Disposer.newDisposable("Disposable for ${BuiltInsSerializer::class.simpleName}.analyzeAndSerialize")
            val messageCollector = createMessageCollector()
            val performanceManager = object : CommonCompilerPerformanceManager(presentableName = "test") {}
            try {
                val configuration = CompilerConfiguration().apply {
                    put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)

                    addKotlinSourceRoots(srcDirs.map { it.path })
                    addJvmClasspathRoots(extraClassPath)
                    configureJdkClasspathRoots()

                    put(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY, destDir)
                    put(CommonConfigurationKeys.MODULE_NAME, "module for built-ins serialization")
                    put(CLIConfigurationKeys.PERF_MANAGER, performanceManager)
                }

                val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

                val serializer = BuiltInsSerializer(configuration, environment, dependOnOldBuiltIns)
                serializer.analyzeAndSerialize()

                onComplete(serializer.totalSize, serializer.totalFiles)
            } finally {
                messageCollector.flush()
                Disposer.dispose(rootDisposable)
            }
        }

        private fun createMessageCollector() = object : GroupingMessageCollector(
            PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false),
            false
        ) {
            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                // Only report diagnostics without a particular location because there's plenty of errors in built-in sources
                // (functions without bodies, incorrect combination of modifiers, etc.)
                if (location == null) {
                    super.report(severity, message, location)
                }
            }
        }
    }

    override fun serialize(analysisResult: CommonAnalysisResult, destDir: File) {
        val files = environment.getSourceFiles()
        val module = analysisResult.moduleDescriptor

        destDir.deleteRecursively()
        if (!destDir.mkdirs()) {
            throw AssertionError("Could not make directories: " + destDir)
        }

        files.map { it.packageFqName }.toSet().forEach { fqName ->
            val packageView = module.getPackage(fqName)
            PackageSerializer(
                packageView.memberScope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) + createCloneable(module),
                packageView.fragments.flatMap { fragment -> DescriptorUtils.getAllDescriptors(fragment.getMemberScope()) },
                packageView.fqName,
                File(destDir, BuiltInSerializerProtocol.getBuiltInsFilePath(packageView.fqName)),
                environment.configuration.languageVersionSettings,
            ).run()
        }
    }

    override fun createSerializerExtension(): KotlinSerializerExtensionBase = BuiltInsSerializerExtension()

    // Serialize metadata for kotlin.Cloneable manually for compatibility with kotlin-reflect 1.0 which expects this metadata to be there.
    // Since Kotlin 1.1, we always discard this class during deserialization (see ClassDeserializer.kt).
    private fun createCloneable(module: ModuleDescriptor): ClassDescriptor {
        val factory = JvmBuiltInClassDescriptorFactory(LockBasedStorageManager.NO_LOCKS, module) {
            EmptyPackageFragmentDescriptor(module, StandardNames.BUILT_INS_PACKAGE_FQ_NAME)
        }
        return factory.createClass(ClassId.topLevel(StandardNames.FqNames.cloneable.toSafe()))
            ?: error("Could not create kotlin.Cloneable in $module")
    }
}
