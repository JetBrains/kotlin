/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtPsiUtil

fun String?.nameAsSafeName(defaultName: String = ""): Name {
    return when {
        this != null -> Name.identifier(KtPsiUtil.unquoteIdentifier(this))
        defaultName.isNotEmpty() -> Name.identifier(defaultName)
        else -> SpecialNames.NO_NAME_PROVIDED
    }
}

fun <T : FirCallBuilder> T.extractArgumentsFrom(container: List<FirExpression>): T {
    argumentList = buildArgumentList {
        arguments += container
    }
    return this
}
