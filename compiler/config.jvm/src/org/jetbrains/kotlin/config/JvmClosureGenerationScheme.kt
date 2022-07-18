/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

enum class JvmClosureGenerationScheme(
    val description: String,
    val minJvmTarget: JvmTarget
) {
    CLASS("class", JvmTarget.JVM_1_6),
    INDY("indy", JvmTarget.JVM_1_8),
    ;

    companion object {
        @JvmStatic
        fun fromString(string: String?): JvmClosureGenerationScheme? {
            val lowerStr = string?.toLowerCaseAsciiOnly() ?: return null
            return values().find { it.description == lowerStr }
        }
    }
}