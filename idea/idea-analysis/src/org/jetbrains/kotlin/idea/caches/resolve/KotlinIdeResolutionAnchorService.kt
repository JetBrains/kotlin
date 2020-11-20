/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.moduleInfo
import org.jetbrains.kotlin.caches.project.cacheInvalidatingOnRootModifications
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.project.libraryToSourceAnalysisEnabled
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.resolve.ResolutionAnchorProvider

/**
 * This component provides capabilities for correct highlighting for projects with source-dependent libraries.
 * The issue with this kind of libraries is that their declarations are resolved by ResolverForProject
 * that have no access to project sources by itself. The necessary path back to project sources can be provided
 * manually for the libraries in project via resolution anchors. Anchor by itself is a source module which is mapped
 * to a library and used during resolution as a fallback.
 */
class KotlinIdeResolutionAnchorService(
    val project: Project
) : ResolutionAnchorProvider {
    override fun getResolutionAnchor(moduleDescriptor: ModuleDescriptor): ModuleDescriptor? {
        if (!project.libraryToSourceAnalysisEnabled) return null

        val moduleToAnchor = ResolutionAnchorCacheService.getInstance(project).resolutionAnchorsForLibraries
        val moduleInfo = moduleDescriptor.moduleInfo ?: return null
        val keyModuleInfo = if (moduleInfo is SourceForBinaryModuleInfo) moduleInfo.binariesModuleInfo else moduleInfo
        val mapped = moduleToAnchor[keyModuleInfo] ?: return null

        return KotlinCacheService.getInstance(project)
            .getResolutionFacadeByModuleInfo(mapped, mapped.platform)
            ?.moduleDescriptor
    }
}
