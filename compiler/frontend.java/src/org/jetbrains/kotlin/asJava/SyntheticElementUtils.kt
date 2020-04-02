/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.PsiMethod
import com.intellij.psi.SyntheticElement
import org.jetbrains.kotlin.resolve.DescriptorUtils

fun isSyntheticValuesOrValueOfMethod(method: PsiMethod): Boolean {
    if (method !is SyntheticElement) return false
    return DescriptorUtils.ENUM_VALUE_OF.asString() == method.name || DescriptorUtils.ENUM_VALUES.asString() == method.name
}
