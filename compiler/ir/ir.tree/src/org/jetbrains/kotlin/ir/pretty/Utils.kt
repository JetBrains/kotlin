/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import kotlin.reflect.KProperty

typealias IrElementBuilderClosure<Builder> = Builder.() -> Unit

internal class SetAtMostOnce<T>(private val defaultValue: T) {

    private var value: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: defaultValue
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        require(value == null) {
            "Method '${Throwable().stackTrace[1].methodName}' can be called at most once"
        }
        this.value = value
    }
}
