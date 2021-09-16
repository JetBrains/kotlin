/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.symbols.markers.KtConstantValue
import org.jetbrains.kotlin.psi.KtExpression

public abstract class KtCompileTimeConstantProvider : KtAnalysisSessionComponent() {
    public abstract fun evaluate(expression: KtExpression): KtConstantValue?
}

public interface KtCompileTimeConstantProviderMixIn : KtAnalysisSessionMixIn {
    public fun KtExpression.evaluate(): KtConstantValue? =
        analysisSession.compileTimeConstantProvider.evaluate(this)
}
