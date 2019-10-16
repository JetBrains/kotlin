/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

enum class Condition {
    EqTrue, EqFalse, EqNull, NotEqNull;

    fun invert(): Condition = when (this) {
        EqTrue -> EqFalse
        EqFalse -> EqTrue
        EqNull -> NotEqNull
        NotEqNull -> EqNull
    }

    override fun toString(): String = when (this) {
        EqTrue -> "== True"
        EqFalse -> "== False"
        EqNull -> "== Null"
        NotEqNull -> "!= Null"
    }
}

fun Boolean.toEqBoolean(): Condition = if (this) Condition.EqTrue else Condition.EqFalse