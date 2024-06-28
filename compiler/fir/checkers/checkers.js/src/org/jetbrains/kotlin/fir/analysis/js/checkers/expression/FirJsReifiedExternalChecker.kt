/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.expression

import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsWebCheckerUtils
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.expression.FirAbstractReifiedExternalChecker

object FirJsReifiedExternalChecker : FirAbstractReifiedExternalChecker(FirJsWebCheckerUtils)
