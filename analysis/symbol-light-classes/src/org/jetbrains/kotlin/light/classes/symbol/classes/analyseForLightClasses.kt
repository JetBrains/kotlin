/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.lifetime.KtAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.psi.KtElement

internal inline fun <R> analyzeForLightClasses(context: KtElement, action: KtAnalysisSession.() -> R): R =
    analyze(context, KtAlwaysAccessibleLifetimeTokenFactory, action)

internal inline fun <R> analyzeForLightClasses(useSiteKtModule: KtModule, crossinline action: KtAnalysisSession.() -> R): R =
    analyze(useSiteKtModule, KtAlwaysAccessibleLifetimeTokenFactory, action)