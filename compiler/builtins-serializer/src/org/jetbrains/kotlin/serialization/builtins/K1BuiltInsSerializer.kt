/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.builtins

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInClassDescriptorFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.metadata.CommonAnalysisResult
import org.jetbrains.kotlin.cli.metadata.K1LegacyMetadataSerializer
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

/**
 * Produces legacy metadata artifact for builtins using K1 compiler
 */
class K1BuiltInsSerializer(
    configuration: CompilerConfiguration,
    environment: KotlinCoreEnvironment,
    dependOnOldBuiltIns: Boolean
) : K1LegacyMetadataSerializer(configuration, environment, dependOnOldBuiltIns, BuiltInsBinaryVersion.INSTANCE) {
    override fun serialize(analysisResult: CommonAnalysisResult, destDir: File): OutputInfo {
        val files = environment.getSourceFiles()
        val module = analysisResult.moduleDescriptor

        destDir.deleteRecursively()
        if (!destDir.mkdirs()) {
            error("Could not make directories: $destDir")
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
        return OutputInfo(totalSize, totalFiles)
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
