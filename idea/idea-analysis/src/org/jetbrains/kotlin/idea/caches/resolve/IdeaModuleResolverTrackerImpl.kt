/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleResolverTracker
import org.jetbrains.kotlin.analyzer.ResolverForProject
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.project.useAnchorServices

class IdeaModuleResolverTrackerImpl(private val project: Project) : ModuleResolverTracker<IdeaModuleInfo>() {
    private val resolversForModules by lazy {
        mutableMapOf<ModuleDescriptor, ResolverForProject<IdeaModuleInfo>>()
    }
    
    @Synchronized
    override fun registerModuleUpdate(
        newModule: ModuleDescriptor,
        oldModule: ModuleDescriptor?,
        resolverForProject: ResolverForProject<IdeaModuleInfo>
    ) {
        if (!project.useAnchorServices) return
        
        if (oldModule != null) resolversForModules.remove(oldModule)
        resolversForModules[newModule] = resolverForProject
    }

    @Synchronized
    override fun findResolverForProjectByModuleDescriptor(moduleDescriptor: ModuleDescriptor): ResolverForProject<IdeaModuleInfo>? {
        if (!project.useAnchorServices) return null
        
        return resolversForModules[moduleDescriptor]
    }
}