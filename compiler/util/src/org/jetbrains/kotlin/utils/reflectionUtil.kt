/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import java.lang.reflect.Field

fun Field.getSafe(obj: Any?): Any? {
    return try {
        val oldIsAccessible = isAccessible

        try {
            isAccessible = true
            get(obj)
        } finally {
            isAccessible = oldIsAccessible
        }
    } catch (e: Throwable) {
        null
    }
}