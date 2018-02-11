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

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.reference.EntryPoint
import com.intellij.codeInspection.reference.RefElement
import com.intellij.openapi.util.DefaultJDOMExternalizer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jdom.Element

class KotlinJUnitStaticEntryPoint(@JvmField var wasSelected: Boolean = true) : EntryPoint() {
    override fun getDisplayName() = "JUnit static methods"

    override fun isSelected() = wasSelected

    override fun isEntryPoint(refElement: RefElement, psiElement: PsiElement) = isEntryPoint(psiElement)

    private val staticJUnitAnnotations = listOf("org.junit.BeforeClass", "org.junit.AfterClass",
                                                "org.junit.runners.Parameterized.Parameters")

    override fun isEntryPoint(psiElement: PsiElement) = psiElement is PsiMethod &&
            AnnotationUtil.isAnnotated(psiElement, staticJUnitAnnotations) &&
            AnnotationUtil.isAnnotated(psiElement, listOf("kotlin.jvm.JvmStatic"))

    override fun readExternal(element: Element) {
        DefaultJDOMExternalizer.readExternal(this, element)
    }

    override fun setSelected(selected: Boolean) {
        this.wasSelected = selected
    }

    override fun writeExternal(element: Element) {
        if (!wasSelected) {
            DefaultJDOMExternalizer.writeExternal(this, element)
        }
    }
}