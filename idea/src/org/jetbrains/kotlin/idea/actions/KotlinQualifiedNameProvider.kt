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

package org.jetbrains.kotlin.idea.actions

import com.intellij.ide.actions.JavaQualifiedNameProvider
import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

class KotlinQualifiedNameProvider: QualifiedNameProvider {
    override fun adjustElementToCopy(element: PsiElement?) = null

    override fun getQualifiedName(element: PsiElement?) = when(element) {
        is KtClassOrObject -> element.fqName?.asString()
        is KtNamedFunction -> getJavaQualifiedName(LightClassUtil.getLightClassMethod(element))

        is KtProperty -> {
            val lightClassPropertyMethods = LightClassUtil.getLightClassPropertyMethods(element)
            val lightElement: PsiElement? = lightClassPropertyMethods.getter ?: lightClassPropertyMethods.backingField
            getJavaQualifiedName(lightElement)
        }
        else -> null
    }

    private fun getJavaQualifiedName(element: PsiElement?) = element?.let { JavaQualifiedNameProvider().getQualifiedName(element) }

    override fun qualifiedNameToElement(fqn: String?, project: Project?) = null

    override fun insertQualifiedName(fqn: String?, element: PsiElement?, editor: Editor?, project: Project?) {
    }
}
