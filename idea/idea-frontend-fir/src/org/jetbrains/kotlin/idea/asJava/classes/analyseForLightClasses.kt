/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava.classes

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyseWithCustomToken
import org.jetbrains.kotlin.idea.frontend.api.tokens.AlwaysAccessibleValidityTokenFactory
import org.jetbrains.kotlin.psi.KtElement

internal inline fun <R> analyseForLightClasses(context: KtElement, action: KtAnalysisSession.() -> R): R =
    analyseWithCustomToken(context, AlwaysAccessibleValidityTokenFactory, action)