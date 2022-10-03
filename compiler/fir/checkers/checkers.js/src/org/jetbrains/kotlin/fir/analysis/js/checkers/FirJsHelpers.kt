/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.isNativeObject
import org.jetbrains.kotlin.fir.declarations.isPredefinedObject
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

fun FirBasedSymbol<*>.isNativeObject(context: CheckerContext) = isNativeObject(context.session)

fun FirBasedSymbol<*>.isPredefinedObject(context: CheckerContext) = isPredefinedObject(context.session)
