/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.configure

import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.android.tools.idea.gradle.project.sync.setup.post.ModuleSetupStep
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.kotlin.idea.configuration.compilerArgumentsBySourceSet
import org.jetbrains.kotlin.idea.configuration.configureFacetByCompilerArguments
import org.jetbrains.kotlin.idea.configuration.sourceSetName
import org.jetbrains.kotlin.idea.facet.KotlinFacet

class KotlinAndroidModuleSetupStep : ModuleSetupStep() {
    override fun setUpModule(module: Module, progressIndicator: ProgressIndicator?) {
        val facet = AndroidFacet.getInstance(module) ?: return
        val androidModel = AndroidModuleModel.get(facet) ?: return
        val sourceSetName = androidModel.selectedVariant.name
        if (module.sourceSetName == sourceSetName) return
        val argsInfo = module.compilerArgumentsBySourceSet?.get(sourceSetName) ?: return
        val kotlinFacet = KotlinFacet.get(module) ?: return
        module.sourceSetName = sourceSetName
        configureFacetByCompilerArguments(kotlinFacet, argsInfo, null)
    }
}