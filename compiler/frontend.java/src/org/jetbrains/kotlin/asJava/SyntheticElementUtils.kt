/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.PsiMethod
import com.intellij.psi.SyntheticElement
import org.jetbrains.kotlin.builtins.StandardNames

fun isSyntheticValuesOrValueOfMethod(method: PsiMethod): Boolean {
    if (method !is SyntheticElement) return false
    return StandardNames.ENUM_VALUE_OF.asString() == method.name || StandardNames.ENUM_VALUES.asString() == method.name
}
