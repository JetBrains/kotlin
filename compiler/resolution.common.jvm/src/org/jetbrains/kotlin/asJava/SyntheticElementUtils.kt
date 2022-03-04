/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.SyntheticElement

fun isSyntheticValuesOrValueOfMethod(method: PsiMethod): Boolean {
    if (method is SyntheticElement) {
        val name = method.name
        if (name == "values" || name == "valueOf") {
            if (method.hasModifierProperty(PsiModifier.PUBLIC) && method.hasModifierProperty(PsiModifier.STATIC)) {
                val parameterCount = method.parameterList.parametersCount
                when (name) {
                    "values" -> if (parameterCount == 0) return true
                    "valueOf" -> if (parameterCount == 1) return true
                }
            }
        }
    }

    return false
}