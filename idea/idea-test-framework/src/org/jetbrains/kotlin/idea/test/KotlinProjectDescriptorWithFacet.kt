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

package org.jetbrains.kotlin.idea.test

import com.intellij.facet.FacetManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.facet.KotlinFacetConfiguration
import org.jetbrains.kotlin.idea.facet.KotlinFacetType
import org.jetbrains.kotlin.test.testFramework.runInEdtAndWait
import org.jetbrains.kotlin.test.testFramework.runWriteAction

class KotlinProjectDescriptorWithFacet(val languageVersion: LanguageVersion) : KotlinLightProjectDescriptor() {
    override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
        val facetManager = FacetManager.getInstance(module)
        val facetModel = facetManager.createModifiableModel()
        val configuration = KotlinFacetConfiguration()
        configuration.settings.useProjectSettings = false
        configuration.settings.versionInfo.languageLevel = languageVersion
        val facet = facetManager.createFacet(KotlinFacetType.INSTANCE, "Kotlin", configuration, null)
        facetModel.addFacet(facet)
        runInEdtAndWait {
            runWriteAction { facetModel.commit() }
        }
    }

    companion object {
        val KOTLIN_10 = KotlinProjectDescriptorWithFacet(LanguageVersion.KOTLIN_1_0)
    }
}
