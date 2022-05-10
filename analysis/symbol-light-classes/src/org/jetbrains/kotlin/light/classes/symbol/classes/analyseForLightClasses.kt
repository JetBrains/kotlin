/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.lifetime.KtAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.psi.KtElement

internal inline fun <R> analyseForLightClasses(context: KtElement, action: KtAnalysisSession.() -> R): R =
    analyze(context, KtAlwaysAccessibleLifetimeTokenFactory, action)