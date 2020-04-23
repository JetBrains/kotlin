/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project

open class ModuleResolverTracker<M : ModuleInfo> {
    open fun registerModuleUpdate(
        newModule: ModuleDescriptor,
        oldModule: ModuleDescriptor?,
        resolverForProject: ResolverForProject<M>
    ) {
    }
    
    open fun findResolverForProjectByModuleDescriptor(moduleDescriptor: ModuleDescriptor): ResolverForProject<M>? = null
    
    companion object {
        val Default = ModuleResolverTracker<ModuleInfo>()

        fun getInstance(project: Project): ModuleResolverTracker<*> =
            ServiceManager.getService(project, ModuleResolverTracker::class.java) ?: Default
    }
}