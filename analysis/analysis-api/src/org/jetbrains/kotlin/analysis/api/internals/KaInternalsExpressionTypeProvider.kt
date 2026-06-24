/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtDeclarationWithReturnType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsExpressionTypeProvider {
    public fun expressionType(expression: KtExpression): KaType?

    public fun returnType(declaration: KtDeclarationWithReturnType): KaType

    public fun functionType(function: KtFunction): KaType

    public fun expectedType(element: PsiElement): KaType?

    public fun isDefinitelyNull(expression: KtExpression): Boolean

    public fun isDefinitelyNotNull(expression: KtExpression): Boolean
}
