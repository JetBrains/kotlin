/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json.base

import kotlin.enums.enumEntries

inline fun <reified T : Enum<T>> (T.() -> String).typeFinder(): (String) -> T {
    val prop = this
    return { name: String -> enumEntries<T>().single { it.prop() == name } }
}
