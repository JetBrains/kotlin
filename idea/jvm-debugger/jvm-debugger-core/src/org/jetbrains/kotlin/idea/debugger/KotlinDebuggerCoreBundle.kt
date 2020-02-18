/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.CommonBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.kotlin.idea.core.util.KotlinBundleBase
import java.util.*

object KotlinDebuggerCoreBundle : KotlinBundleBase() {
    @NonNls
    private const val BUNDLE = "org.jetbrains.kotlin.idea.debugger.KotlinDebuggerCoreBundle"

    override fun createBundle(): ResourceBundle = ResourceBundle.getBundle(BUNDLE)

    @JvmStatic
    fun message(@NonNls @PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?): String {
        return CommonBundle.message(bundle, key, *params)
    }
}