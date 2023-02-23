/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class KtStringTemplateExpressionElementType(@NonNls debugName: String) :
    KtPlaceHolderStubElementType<KtStringTemplateExpression>(debugName, KtStringTemplateExpression::class.java) {
}
