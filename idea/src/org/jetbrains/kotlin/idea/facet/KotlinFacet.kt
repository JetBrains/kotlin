/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetManager.getInstance
import com.intellij.openapi.module.Module

class KotlinFacet(
        module: Module,
        name: String,
        configuration: KotlinFacetConfiguration
) : Facet<KotlinFacetConfiguration>(KotlinFacetType.INSTANCE, module, name, configuration, null) {
    companion object {
        fun get(module: Module): KotlinFacet? {
            if (module.isDisposed) return null
            return getInstance(module).getFacetByType<KotlinFacet>(KotlinFacetType.TYPE_ID)
        }
    }
}