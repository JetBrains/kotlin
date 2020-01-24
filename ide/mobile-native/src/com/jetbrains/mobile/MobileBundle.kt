/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE_PATH = "messages.MobileBundle"

object MobileBundle : AbstractBundle(BUNDLE_PATH) {
    fun message(@PropertyKey(resourceBundle = BUNDLE_PATH) key: String, vararg params: Any): String {
        return getMessage(key, *params)
    }
}