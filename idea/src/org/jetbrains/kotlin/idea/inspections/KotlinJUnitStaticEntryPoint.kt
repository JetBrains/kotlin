/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

    private val staticJUnitAnnotations = listOf(
        "org.junit.BeforeClass",
        "org.junit.AfterClass",
        "org.junit.runners.Parameterized.Parameters"
    )

    override fun isEntryPoint(psiElement: PsiElement) = psiElement is PsiMethod &&
            AnnotationUtil.isAnnotated(psiElement, staticJUnitAnnotations, AnnotationUtil.CHECK_TYPE) &&
            AnnotationUtil.isAnnotated(psiElement, listOf("kotlin.jvm.JvmStatic"), AnnotationUtil.CHECK_TYPE)

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