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

package org.jetbrains.kotlin.idea.spring.generate

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.xml.XmlFile
import com.intellij.spring.SpringBundle
import com.intellij.spring.SpringManager
import com.intellij.spring.model.actions.generate.GenerateSpringBeanDependenciesUtil
import com.intellij.spring.model.utils.SpringModelUtils
import icons.SpringApiIcons
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateActionBase
import org.jetbrains.kotlin.idea.editor.BatchTemplateRunner
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

abstract class GenerateKotlinSpringBeanDependencyAction(
        text: String,
        private val injectionKind: SpringDependencyInjectionKind
) : KotlinGenerateActionBase() {
    class Constructor: GenerateKotlinSpringBeanDependencyAction(
            SpringBundle.message("action.Spring.Beans.Generate.Constructor.Dependency.Action.text"),
            SpringDependencyInjectionKind.CONSTRUCTOR
    )
    class Setter: GenerateKotlinSpringBeanDependencyAction(
            SpringBundle.message("action.Spring.Beans.Generate.Setter.Dependency.Action.text"),
            SpringDependencyInjectionKind.SETTER
    )
    class LateinitProperty: GenerateKotlinSpringBeanDependencyAction(
            "Spring 'lateinit' Dependency...",
            SpringDependencyInjectionKind.LATEINIT_PROPERTY
    )

    init {
        templatePresentation.text = text
        templatePresentation.icon = SpringApiIcons.Spring
    }

    override fun isValidForClass(targetClass: KtClassOrObject): Boolean {
        if (targetClass !is KtClass || targetClass.isInterface() || targetClass.isEnum() || targetClass.fqName == null) return false
        val lightClass = targetClass.toLightClass() ?: return false
        val module = GenerateSpringBeanDependenciesUtil.getSpringModule(lightClass) ?: return false
        return SpringManager.getInstance(module.project).getCombinedModel(module).configFiles.any { it is XmlFile }
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val klass = getTargetClass(editor, file) as? KtClass ?: return
        val lightClass = klass.toLightClass() ?: return
        val springModel = SpringModelUtils.getInstance().getPsiClassSpringModel(lightClass)
        val templates = project.executeWriteCommand<List<BatchTemplateRunner>>("") {
            generateDependenciesFor(springModel, lightClass, injectionKind)
        }
        templates.forEach { it.runTemplates() }
    }
}