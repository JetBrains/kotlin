/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo.model

import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.DiagnosticCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.diagnostics.Diagnostic

class DiagnosticCodeMetaInfo(
    override val start: Int,
    override val end: Int,
    override val renderConfiguration: DiagnosticCodeMetaInfoRenderConfiguration,
    val diagnostic: Diagnostic
) : CodeMetaInfo {
    override val platforms: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)

    override fun getTag(): String = renderConfiguration.getTag(this)
}
