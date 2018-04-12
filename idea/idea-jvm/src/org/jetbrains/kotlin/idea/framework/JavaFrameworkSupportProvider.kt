/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.configuration.BuildSystemType
import org.jetbrains.kotlin.idea.configuration.getBuildSystemType
import javax.swing.JComponent

class JavaFrameworkSupportProvider : FrameworkSupportInModuleProvider() {
    override fun getFrameworkType(): FrameworkTypeEx = JavaFrameworkType.instance

    override fun createConfigurable(model: FrameworkSupportModel): FrameworkSupportInModuleConfigurable {
        return object : FrameworkSupportInModuleConfigurable() {
            private var description: JavaRuntimeLibraryDescription? = null

            override fun createLibraryDescription(): CustomLibraryDescription? {
                description = JavaRuntimeLibraryDescription(model.project)
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
                        JSLibraryStdDescription.SUITABLE_LIBRARY_KINDS,
                        JSFrameworkType.instance)

                description!!.finishLibConfiguration(module, rootModel)
            }

            override fun onFrameworkSelectionChanged(selected: Boolean) {
                if (selected) {
                    val providerId = JSFrameworkType.instance.id
                    if (model.isFrameworkSelected(providerId)) {
                        model.setFrameworkComponentEnabled(providerId, false)
                    }
                }
            }
        }
    }

    override fun isEnabledForModuleType(moduleType: ModuleType<*>): Boolean = moduleType is JavaModuleType

    override fun canAddSupport(module: Module, facetsProvider: FacetsProvider): Boolean {
        return super.canAddSupport(module, facetsProvider) && module.getBuildSystemType() == BuildSystemType.JPS
    }
}
