/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo.model

import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.AbstractCodeMetaInfoRenderConfiguration

interface CodeMetaInfo {
    val start: Int
    val end: Int
    val tag: String
    val renderConfiguration: AbstractCodeMetaInfoRenderConfiguration
    val platforms: MutableList<String>

    fun asString(): String
}
