package org.jetbrains.kotlin.utils

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun <T> SmartSet<T>.toSmartList(): SmartList<T> {
    return SmartList<T>().also {
        for (element in this) {
            it.add(element)
        }
    }
}
