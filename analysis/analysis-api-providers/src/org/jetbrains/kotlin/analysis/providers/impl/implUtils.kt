/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementFinder

public inline fun forEachNonKotlinPsiElementFinder(project: Project, action: (PsiElementFinder) -> Unit) {
    for (finder in PsiElementFinder.EP.getPoint(project).extensionList) {
        if (finder::class.java.name == KOTLIN_JAVA_ELEMENT_FINDER_CLASS_NAME) {
            continue
        }
        action(finder)
    }
}

public const val KOTLIN_JAVA_ELEMENT_FINDER_CLASS_NAME: String = "org.jetbrains.kotlin.asJava.finder.JavaElementFinder"
