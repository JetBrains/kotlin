/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSimpleConstantValue
import org.jetbrains.kotlin.psi.KtExpression

abstract class KtCompileTimeConstantProvider : KtAnalysisSessionComponent() {
    abstract fun evaluate(expression: KtExpression): KtSimpleConstantValue<*>?
}

interface KtCompileTimeConstantProviderMixIn : KtAnalysisSessionMixIn {
    fun KtExpression.evaluate(): KtSimpleConstantValue<*>? =
        analysisSession.compileTimeConstantProvider.evaluate(this)
}
