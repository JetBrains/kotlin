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

import com.intellij.openapi.module.Module
import com.intellij.spring.model.actions.generate.GenerateSpringBeanDependenciesUtil
import icons.SpringApiIcons
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateActionBase
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

abstract class GenerateKotlinSpringBeanDependencyAction(text: String) : KotlinGenerateActionBase() {
    init {
        templatePresentation.text = text
        templatePresentation.icon = SpringApiIcons.Spring
    }

    override fun isValidForClass(targetClass: KtClassOrObject): Boolean {
        if (targetClass !is KtClass || targetClass.isInterface() || targetClass.isEnum() || targetClass.fqName == null) return false
        val lightClass = targetClass.toLightClass() ?: return false
        val module = GenerateSpringBeanDependenciesUtil.getSpringModule(lightClass) ?: return false
        return checkContext(module)
    }

    abstract fun checkContext(module: Module): Boolean
}