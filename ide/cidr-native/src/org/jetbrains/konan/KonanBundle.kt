/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

private const val PATH_TO_BUNDLE: String = "KonanBundle"

object KonanBundle : AbstractBundle(PATH_TO_BUNDLE) {
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): String {
        return getMessage(key, *params)
    }
}