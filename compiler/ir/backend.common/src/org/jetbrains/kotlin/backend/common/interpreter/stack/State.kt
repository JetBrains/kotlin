/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.ir.expressions.IrConst

interface State {
    fun getState(): List<State>
    fun getName(): String
}

class Primitive<T>(public val varName: String, public val value: IrConst<T>) : State {
    override fun getState(): List<State> {
        return listOf(this)
    }

    override fun getName(): String {
        return varName
    }
}
class Complex(public val objName: String, public val values: List<State>) : State {
    override fun getState(): List<State> {
        return values
    }

    override fun getName(): String {
        return objName
    }
}