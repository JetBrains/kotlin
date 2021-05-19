/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.AbstractCodeMetaInfoRenderConfiguration

class IrInterpreterCodeMetaInfo(override val start: Int, override val end: Int, val description: String, isError: Boolean) : CodeMetaInfo {
    override var renderConfiguration = RenderConfiguration()

    override val tag: String = if (isError) "WAS_NOT_EVALUATED" else "EVALUATED"

    override val attributes: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)

    class RenderConfiguration : AbstractCodeMetaInfoRenderConfiguration() {
        override fun asString(codeMetaInfo: CodeMetaInfo): String {
            codeMetaInfo as IrInterpreterCodeMetaInfo
            return "${super.asString(codeMetaInfo)}: `${codeMetaInfo.description}`"
        }
    }
}