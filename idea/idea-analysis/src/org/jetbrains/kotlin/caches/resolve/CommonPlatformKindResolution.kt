/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caches.resolve

import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analyzer.ResolverForModuleFactory
import org.jetbrains.kotlin.analyzer.common.CommonAnalyzerFacade
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.idea.framework.CommonLibraryKind
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment

class CommonPlatformKindResolution : IdePlatformKindResolution {
    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return virtualFile.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION
    }

    override val libraryKind: PersistentLibraryKind<*>?
        get() = CommonLibraryKind

    override val kind get() = CommonIdePlatformKind

    override val resolverForModuleFactory: ResolverForModuleFactory
        get() = CommonAnalyzerFacade

    override fun createBuiltIns(settings: PlatformAnalysisSettings, projectContext: ProjectContext): KotlinBuiltIns {
        return DefaultBuiltIns.Instance
    }
}