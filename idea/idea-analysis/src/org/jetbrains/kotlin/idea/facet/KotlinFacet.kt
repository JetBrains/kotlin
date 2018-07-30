/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetManager
import com.intellij.openapi.module.Module

class KotlinFacet(
    module: Module,
    name: String,
    configuration: KotlinFacetConfiguration
) : Facet<KotlinFacetConfiguration>(KotlinFacetType.INSTANCE, module, name, configuration, null) {
    companion object {
        fun get(module: Module): KotlinFacet? {
            if (module.isDisposed) return null
            return FacetManager.getInstance(module).getFacetByType<KotlinFacet>(KotlinFacetType.TYPE_ID)
        }
    }
}
