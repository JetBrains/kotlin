/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo.renderConfigurations

import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.utils.StringUtilsKt

abstract class AbstractCodeMetaInfoRenderConfiguration(var renderParams: Boolean = true) {
    open fun asString(codeMetaInfo: CodeMetaInfo): String = codeMetaInfo.tag + getAttributesString(codeMetaInfo)

    open fun getAdditionalParams(codeMetaInfo: CodeMetaInfo) = ""

    open fun postProcessAttributes(codeMetaInfo: CodeMetaInfo) {}

    protected fun getAttributesString(codeMetaInfo: CodeMetaInfo): String {
        if (codeMetaInfo.attributes.isEmpty()) return ""
        return "{${codeMetaInfo.attributes.joinToString(";")}}"
    }
}
