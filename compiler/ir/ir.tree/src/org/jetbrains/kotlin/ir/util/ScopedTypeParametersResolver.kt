/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import java.util.*

interface TypeParametersResolver {
    fun enterTypeParameterScope(typeParametersContainer: IrTypeParametersContainer)
    fun leaveTypeParameterScope()

    @DescriptorBasedIr
    fun resolveScopedTypeParameter(typeParameterDescriptor: TypeParameterDescriptor): IrTypeParameterSymbol?
}

@OptIn(DescriptorBasedIr::class)
class ScopedTypeParametersResolver : TypeParametersResolver {

    private val typeParameterScopes = ArrayDeque<Map<TypeParameterDescriptor, IrTypeParameterSymbol>>()

    override fun enterTypeParameterScope(typeParametersContainer: IrTypeParametersContainer) {
        typeParameterScopes.addFirst(
            typeParametersContainer.typeParameters.associate {
                it.descriptor to it.symbol
            }
        )
    }

    override fun leaveTypeParameterScope() {
        typeParameterScopes.removeFirst()
    }

    @DescriptorBasedIr
    override fun resolveScopedTypeParameter(typeParameterDescriptor: TypeParameterDescriptor): IrTypeParameterSymbol? {
        for (scope in typeParameterScopes) {
            val local = scope[typeParameterDescriptor]
            if (local != null) return local
        }
        return null
    }
}