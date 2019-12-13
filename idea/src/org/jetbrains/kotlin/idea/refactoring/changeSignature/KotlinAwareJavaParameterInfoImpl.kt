/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiType
import com.intellij.refactoring.changeSignature.ParameterInfoImpl
import org.jetbrains.kotlin.psi.KtExpression

class KotlinAwareJavaParameterInfoImpl(
    oldParameterIndex: Int,
    name: String,
    type: PsiType,
    val kotlinDefaultValue: KtExpression?
) : ParameterInfoImpl(oldParameterIndex, name, type, kotlinDefaultValue?.text ?: "")