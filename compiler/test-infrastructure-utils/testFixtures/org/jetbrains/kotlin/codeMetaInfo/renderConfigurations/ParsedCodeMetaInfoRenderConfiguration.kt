/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo.renderConfigurations

import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo

object ParsedCodeMetaInfoRenderConfiguration : AbstractCodeMetaInfoRenderConfiguration() {
    override fun asString(codeMetaInfo: CodeMetaInfo): String {
        require(codeMetaInfo is ParsedCodeMetaInfo)
        return super.asString(codeMetaInfo) + (codeMetaInfo.description?.let { "(\"$it\")" } ?: "")
    }
}
