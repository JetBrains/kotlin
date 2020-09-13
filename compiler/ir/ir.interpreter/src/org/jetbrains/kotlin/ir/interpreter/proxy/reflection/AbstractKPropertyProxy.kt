/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.state.KPropertyState
import org.jetbrains.kotlin.ir.interpreter.state.State
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KVisibility

internal abstract class AbstractKPropertyProxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : ReflectionProxy, KProperty<State> {
    private val propertyOwner: IrProperty
        get() = state.propertyReference.symbol.owner

    override val isAbstract: Boolean
        get() = propertyOwner.modality == Modality.ABSTRACT
    override val isConst: Boolean
        get() = propertyOwner.isConst
    override val isFinal: Boolean
        get() = propertyOwner.modality == Modality.FINAL
    override val isLateinit: Boolean
        get() = propertyOwner.isLateinit
    override val isOpen: Boolean
        get() = propertyOwner.modality == Modality.OPEN
    override val isSuspend: Boolean
        get() = TODO("Not yet implemented")
    override val name: String
        get() = propertyOwner.name.asString()

    override val annotations: List<Annotation>
        get() = TODO("Not yet implemented")
    override val returnType: KType
        get() = TODO("Not yet implemented")
    override val visibility: KVisibility?
        get() = TODO("Not yet implemented")

    override fun equals(other: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun hashCode(): Int {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        TODO("Not yet implemented")
    }
}