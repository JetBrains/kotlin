/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run.multiplatform

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.MultipleRunLocationsProvider
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.config.KotlinModuleKind
import org.jetbrains.kotlin.idea.caches.project.implementingModules
import org.jetbrains.kotlin.idea.caches.project.isNewMPPModule
import org.jetbrains.kotlin.idea.configuration.toModuleGroup
import org.jetbrains.kotlin.idea.facet.KotlinFacet

class KotlinMultiplatformRunLocationsProvider : MultipleRunLocationsProvider() {
    override fun getLocationDisplayName(locationCreatedFrom: Location<*>, originalLocation: Location<*>): String? {
        val module = locationCreatedFrom.module ?: return null
        return "[${compactedGradleProjectId(module) ?: module.name}]"
    }

    override fun getAlternativeLocations(originalLocation: Location<*>): List<Location<*>> {
        val originalModule = originalLocation.module ?: return emptyList()
        return modulesToRunFrom(originalModule).map { PsiLocation(originalLocation.project, it, originalLocation.psiElement) }
    }
}

private fun compactedGradleProjectId(module: Module): String? {
    if (module.isNewMPPModule) {
        // TODO: more robust way to get compilation/sourceSet name
        return module.name.substringAfterLast('_')
    } else {
        return module.toModuleGroup().baseModule.name
    }
}

private fun modulesToRunFrom(originalModule: Module): List<Module> {
    val modules = originalModule.implementingModules
    return if (originalModule.isNewMPPModule) {
        modules.filter { KotlinFacet.get(it)?.configuration?.settings?.kind == KotlinModuleKind.COMPILATION_AND_SOURCE_SET_HOLDER }
    } else {
        modules
    }
}