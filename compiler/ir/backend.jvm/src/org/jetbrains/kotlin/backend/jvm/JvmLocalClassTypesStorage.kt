/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.LocalClassDataStorage
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.org.objectweb.asm.Type
import java.util.concurrent.ConcurrentHashMap

class JvmLocalClassTypesStorage : LocalClassDataStorage<Type> {
    private val localClassTypes = ConcurrentHashMap<IrAttributeContainer, Type>()

    override fun get(declaration: IrAttributeContainer): Type? = localClassTypes[declaration.attributeOwnerId]

    override fun set(declaration: IrAttributeContainer, localClassData: Type) {
        localClassTypes[declaration.attributeOwnerId] = localClassData
    }

    override fun getAsString(declaration: IrAttributeContainer): String? = this[declaration]?.internalName
}
