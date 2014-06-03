/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.framework

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.platform.ProjectTemplatesFactory
import com.intellij.platform.templates.BuilderBasedTemplate
import org.jetbrains.jet.plugin.JetIcons
import org.jetbrains.jet.plugin.project.TargetPlatform

public class KotlinTemplatesFactory : ProjectTemplatesFactory() {
    class object {
        public val KOTLIN_GROUP_NAME: String = "Kotlin"
    }

    override fun getGroups() = array(KOTLIN_GROUP_NAME)
    override fun getGroupIcon(group: String) = JetIcons.SMALL_LOGO

    override fun createTemplates(group: String, context: WizardContext?) =
        array(
                BuilderBasedTemplate(KotlinModuleBuilder(TargetPlatform.JVM, "Kotlin - JVM", "Kotlin module for JVM target")),
                BuilderBasedTemplate(KotlinModuleBuilder(TargetPlatform.JS, "Kotlin - JavaScript", "Kotlin module for JavaScript target"))
        )
}
