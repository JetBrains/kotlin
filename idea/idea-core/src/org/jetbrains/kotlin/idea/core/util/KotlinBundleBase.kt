/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.util

import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.*

abstract class KotlinBundleBase {
    private var bundleCache: Reference<ResourceBundle>? = null

    protected abstract fun createBundle(): ResourceBundle

    val bundle: ResourceBundle
        get() {
            val cachedValue = bundleCache?.get()
            if (cachedValue != null) {
                return cachedValue
            }

            val newValue = createBundle()
            bundleCache = SoftReference(newValue)
            return newValue
        }
}