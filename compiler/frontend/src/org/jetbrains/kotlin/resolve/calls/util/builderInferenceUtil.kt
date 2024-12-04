/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.util

import org.jetbrains.kotlin.psi.KtLambdaExpression

class BuilderLambdaLabelingInfo(val builderLambda: KtLambdaExpression?) {
    companion object {
        val EMPTY = BuilderLambdaLabelingInfo(null)
    }

    override fun toString(): String {
        return builderLambda.toString()
    }
}
