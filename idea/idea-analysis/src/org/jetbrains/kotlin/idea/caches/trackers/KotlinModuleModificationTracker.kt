/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.trackers

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.CommonProcessors
import org.jetbrains.kotlin.idea.caches.project.cached

class KotlinModuleModificationTracker(val module: Module) : ModificationTracker {
    private val kotlinModCountListener =
        KotlinCodeBlockModificationListener.getInstance(module.project)
    private val psiModificationTracker = PsiModificationTracker.SERVICE.getInstance(module.project)
    private val dependencies by lazy {
        module.cached(CachedValueProvider {
            CachedValueProvider.Result.create(
                HashSet<Module>().apply {
                    ModuleRootManager.getInstance(module).orderEntries().recursively().forEachModule(
                        CommonProcessors.CollectProcessor(this)
                    )
                },
                ProjectRootModificationTracker.getInstance(module.project)
            )
        })
    }

    override fun getModificationCount(): Long {
        val currentGlobalCount = psiModificationTracker.outOfCodeBlockModificationCount

        if (kotlinModCountListener.hasPerModuleModificationCounts()) {
            val selfCount = kotlinModCountListener.getModificationCount(module)
            if (selfCount == currentGlobalCount) return selfCount
            var maxCount = selfCount
            for (dependency in dependencies) {
                val depCount = kotlinModCountListener.getModificationCount(dependency)
                if (depCount == currentGlobalCount) return currentGlobalCount
                if (depCount > maxCount) maxCount = depCount
            }
            return maxCount
        }
        return currentGlobalCount
    }
}