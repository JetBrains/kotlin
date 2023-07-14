/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.config

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext

abstract class FirLanguageVersionSettingsChecker {
    /**
     * This API allows us to check language version settings independently of particular code pieces.
     *
     * [rawReport] allows to report a diagnostic directly to a message collector.
     * This function accepts isError: Boolean and message: String as parameters.
     */
    abstract fun check(context: CheckerContext, rawReport: (Boolean, String) -> Unit)
}
