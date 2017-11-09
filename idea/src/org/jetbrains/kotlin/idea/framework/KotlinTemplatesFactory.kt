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

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.platform.ProjectTemplate
import com.intellij.platform.ProjectTemplatesFactory
import com.intellij.platform.templates.BuilderBasedTemplate
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

class KotlinTemplatesFactory : ProjectTemplatesFactory() {
    companion object {
        val EP_NAME = ExtensionPointName.create<ModuleBuilder>("org.jetbrains.kotlin.moduleBuilder")

        val KOTLIN_GROUP_NAME: String = "Kotlin"
    }

    override fun getGroups() = arrayOf(KOTLIN_GROUP_NAME)
    override fun getGroupIcon(group: String) = KotlinIcons.SMALL_LOGO

    override fun createTemplates(group: String?, context: WizardContext?): Array<out ProjectTemplate> {
        val result = mutableListOf<ProjectTemplate>(
                BuilderBasedTemplate(KotlinModuleBuilder(JvmPlatform,
                                                         "Kotlin/JVM",
                                                         "Kotlin module for JVM target",
                                                         KotlinIcons.SMALL_LOGO)),

                BuilderBasedTemplate(KotlinModuleBuilder(JsPlatform, "Kotlin/JS",
                                                         "Kotlin module for JavaScript target",
                                                         KotlinIcons.JS)
                )
        )
        result.addAll(Extensions.getExtensions(EP_NAME).map { BuilderBasedTemplate(it) })
        return result.toTypedArray()
    }
}
