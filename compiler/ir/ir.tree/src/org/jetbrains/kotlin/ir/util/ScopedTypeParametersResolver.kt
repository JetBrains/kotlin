/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import java.util.*

class ScopedTypeParametersResolver {

    private val typeParameterScopes = ArrayDeque<Map<TypeParameterDescriptor, IrTypeParameterSymbol>>()

    fun enterTypeParameterScope(typeParametersContainer: IrTypeParametersContainer) {
        typeParameterScopes.addFirst(
            typeParametersContainer.typeParameters.associate {
                it.descriptor to it.symbol
            }
        )
    }

    fun leaveTypeParameterScope() {
        typeParameterScopes.removeFirst()
    }

    fun resolveScopedTypeParameter(typeParameterDescriptor: TypeParameterDescriptor): IrTypeParameterSymbol? {
        for (scope in typeParameterScopes) {
            val local = scope[typeParameterDescriptor]
            if (local != null) return local
        }
        return null
    }
}