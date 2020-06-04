/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.util.registry.Registry

// Just a wrapper to see whether resolve works via FIR or not
object FirResolution {
    private const val optionName = "kotlin.use.fir.resolution"

    private val initialEnabledValue: Boolean by lazy {
        Registry.`is`(optionName, /* defaultValue = */ true)
    }

    private var changedEnabledValue: Boolean? = null

    var enabled: Boolean
        get() = changedEnabledValue ?: initialEnabledValue
        set(value) {
            changedEnabledValue = value
        }
}