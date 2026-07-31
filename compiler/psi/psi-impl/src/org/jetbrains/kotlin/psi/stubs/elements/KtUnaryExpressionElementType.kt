/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import org.jetbrains.kotlin.psi.KtUnaryExpression

internal class KtUnaryExpressionElementType<T : KtUnaryExpression>(
    debugName: String,
    psiClass: Class<T>,
) : KtPlaceHolderStubElementType<T>(debugName, psiClass)
