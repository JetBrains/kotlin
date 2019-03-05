/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caches.resolve

import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analyzer.ResolverForModuleFactory
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.idea.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.idea.framework.JSLibraryKind
import org.jetbrains.kotlin.js.resolve.JsResolverForModuleFactory
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.js.resolve.JsPlatformCompilerServices
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind

class JsPlatformKindResolution : IdePlatformKindResolution {
    override fun isLibraryFileForPlatform(virtualFile: VirtualFile): Boolean {
        return virtualFile.extension == "js" || virtualFile.extension == "kjsm"
    }

    override val libraryKind: PersistentLibraryKind<*>?
        get() = JSLibraryKind

    override val kind get() = JsIdePlatformKind

    override val resolverForModuleFactory: ResolverForModuleFactory
        get() = JsResolverForModuleFactory

    override fun createBuiltIns(settings: PlatformAnalysisSettings, projectContext: ProjectContext): KotlinBuiltIns {
        return JsPlatformCompilerServices.builtIns
    }
}
