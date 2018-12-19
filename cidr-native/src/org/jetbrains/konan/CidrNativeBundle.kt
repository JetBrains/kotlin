/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

class CidrNativeBundle : AbstractBundle(PATH_TO_BUNDLE) {

    companion object {
        private const val PATH_TO_BUNDLE: String = "org.jetbrains.konan.CidrNativeBundle"
        private val BUNDLE = CidrNativeBundle()

        fun message(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): String {
            return BUNDLE.getMessage(key, *params)
        }
    }

}