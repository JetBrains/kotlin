/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caches.resolve

import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.PlatformAnalysisParameters
import org.jetbrains.kotlin.analyzer.ResolverForModuleFactory
import org.jetbrains.kotlin.analyzer.common.CommonAnalysisParameters
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.idea.caches.project.SdkInfo
import org.jetbrains.kotlin.idea.caches.resolve.BuiltInsCacheKey
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment

class CommonPlatformKindResolution : IdePlatformKindResolution {
    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return virtualFile.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION
    }

    override val libraryKind: PersistentLibraryKind<*>?
        get() = CommonLibraryKind

    override val kind get() = CommonIdePlatformKind

    override fun getKeyForBuiltIns(moduleInfo: ModuleInfo): BuiltInsCacheKey = BuiltInsCacheKey.DefaultBuiltInsKey

    override fun createBuiltIns(moduleInfo: ModuleInfo, projectContext: ProjectContext, sdkDependency: SdkInfo?): KotlinBuiltIns {
        return DefaultBuiltIns.Instance
    }

    override fun createResolverForModuleFactory(
        settings: PlatformAnalysisParameters,
        environment: TargetEnvironment,
        platform: TargetPlatform
    ): ResolverForModuleFactory {
        return CommonResolverForModuleFactory(
            settings as CommonAnalysisParameters,
            environment,
            platform,
            shouldCheckExpectActual = true
        )
    }
}