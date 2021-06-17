/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private val HIDDEN_SINCE_NAME = Name.identifier("hiddenSince")

private object FirDeprecatedSinceVersionKey : FirDeclarationDataKey()

private var FirCallableDeclaration<*>.deprecatedSinceKotlinCachedValue: DeprecatedSinceValue? by FirDeclarationDataRegistry.data(
    FirDeprecatedSinceVersionKey
)

private fun FirAnnotationContainer.getHiddenSinceKotlin(): ApiVersion? =
    getAnnotationByFqName(StandardNames.FqNames.deprecatedSinceKotlin)?.getVersionFromArgument(HIDDEN_SINCE_NAME)

fun <T> T.getHiddenSinceKotlinCached(): ApiVersion? where T : FirCallableDeclaration<*>, T : FirAnnotationContainer {
    val cached = deprecatedSinceKotlinCachedValue
    if (cached != null) return cached.hiddenSince
    val calculated = getHiddenSinceKotlin()
    deprecatedSinceKotlinCachedValue = DeprecatedSinceValue(calculated)
    return calculated
}


private fun FirAnnotationCall.getVersionFromArgument(name: Name): ApiVersion? =
    findArgumentByName(name)?.let { expression ->
        expression.safeAs<FirConstExpression<*>>()?.value.safeAs<String>()?.let { ApiVersion.parse(it) }
    }

private class DeprecatedSinceValue(val hiddenSince: ApiVersion?)





