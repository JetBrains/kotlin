/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

object JvmAnalysisFlags {
    @JvmStatic
    val strictMetadataVersionSemantics by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val jsr305 by AnalysisFlag.Delegates.Jsr305StateWarnByDefault

    @JvmStatic
    val jvmDefaultMode by AnalysisFlag.Delegates.JvmDefaultModeDisabledByDefault

    @JvmStatic
    val inheritMultifileParts by AnalysisFlag.Delegates.Boolean

    @JvmStatic
    val sanitizeParentheses by AnalysisFlag.Delegates.Boolean
}
