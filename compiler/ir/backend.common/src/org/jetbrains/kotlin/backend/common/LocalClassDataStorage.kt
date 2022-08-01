/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer

interface LocalClassDataStorage<D> {
    operator fun get(declaration: IrAttributeContainer): D?
    operator fun set(declaration: IrAttributeContainer, localClassData: D)

    fun getAsString(declaration: IrAttributeContainer): String?

    fun copy(source: IrAttributeContainer, destination: IrAttributeContainer) {
        this[source]?.let { data -> this[destination] = data }
    }

    class ClassNames : LocalClassDataStorage<String>, Function1<IrAttributeContainer, String?> {
        private val localClassNames = mutableMapOf<IrAttributeContainer, String>()

        override fun get(declaration: IrAttributeContainer): String? = localClassNames[declaration.attributeOwnerId]

        override fun set(declaration: IrAttributeContainer, localClassData: String) {
            localClassNames[declaration.attributeOwnerId] = localClassData
        }

        override fun getAsString(declaration: IrAttributeContainer): String? = this[declaration]
        override fun invoke(declaration: IrAttributeContainer): String? = this[declaration]
    }
}
