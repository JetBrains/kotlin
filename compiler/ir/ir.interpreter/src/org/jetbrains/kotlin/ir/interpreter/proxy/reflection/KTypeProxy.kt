/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KClassState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KTypeParameterState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KTypeState
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

internal class KTypeProxy(override val state: KTypeState, override val interpreter: IrInterpreter) : ReflectionProxy, KType {
    override val classifier: KClassifier?
        get() = when (val classifier = state.irType.classifierOrFail.owner) {
            is IrClass -> KClassProxy(KClassState(classifier), interpreter)
            is IrTypeParameter -> KTypeParameterProxy(KTypeParameterState(classifier), interpreter)
            else -> TODO()
        }
    override val arguments: List<KTypeProjection>
        get() = state.getArguments(interpreter)
    override val isMarkedNullable: Boolean
        get() = state.irType.isMarkedNullable()
    override val annotations: List<Annotation>
        get() = TODO("Not yet implemented")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KTypeProxy) return false

        return state == other.state
    }

    override fun hashCode(): Int = state.hashCode()

    override fun toString(): String = state.toString()
}