/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.framework

import com.intellij.framework.FrameworkTypeEx
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.FacetsProvider
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import javax.swing.JComponent

class JSFrameworkSupportProvider : FrameworkSupportInModuleProvider() {
    override fun getFrameworkType(): FrameworkTypeEx = JSFrameworkType.instance

    override fun createConfigurable(model: FrameworkSupportModel): FrameworkSupportInModuleConfigurable {
        return object : FrameworkSupportInModuleConfigurable() {
            lateinit var description: JSLibraryStdDescription

            override fun createLibraryDescription(): CustomLibraryDescription? {
                description = JSLibraryStdDescription(model.project)
                return description
            }

            override fun createComponent(): JComponent? = null

            override fun isOnlyLibraryAdded(): Boolean = true

            override fun addSupport(
                    module: Module,
                    rootModel: ModifiableRootModel,
                    modifiableModelsProvider: ModifiableModelsProvider) {
                FrameworksCompatibilityUtils.suggestRemoveIncompatibleFramework(
                        rootModel,
                        JavaRuntimeLibraryDescription.SUITABLE_LIBRARY_KINDS,
                        JavaFrameworkType.getInstance())

                description.finishLibConfiguration(module, rootModel)
            }

            override fun onFrameworkSelectionChanged(selected: Boolean) {
                if (selected) {
                    val providerId = JavaFrameworkType.getInstance().id
                    if (model.isFrameworkSelected(providerId)) {
                        model.setFrameworkComponentEnabled(providerId, false)
                    }
                }
            }
        }
    }

    override fun isEnabledForModuleType(moduleType: ModuleType<*>): Boolean = moduleType is JavaModuleType

    override fun canAddSupport(module: Module, facetsProvider: FacetsProvider): Boolean {
        return super.canAddSupport(module, facetsProvider) &&
               !KotlinPluginUtil.isMavenModule(module) &&
               !KotlinPluginUtil.isGradleModule(module)
    }
}
