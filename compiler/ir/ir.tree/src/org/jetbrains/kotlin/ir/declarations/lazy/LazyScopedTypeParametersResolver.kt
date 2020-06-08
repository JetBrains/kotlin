/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.TypeParametersResolver
import java.util.*

class LazyScopedTypeParametersResolver(private val symbolTable: ReferenceSymbolTable) : TypeParametersResolver {

    private val typeParameterScopes = ArrayDeque<IrTypeParametersContainer>()

    override fun enterTypeParameterScope(typeParametersContainer: IrTypeParametersContainer) {
        typeParameterScopes.addFirst(
            typeParametersContainer
        )
    }

    override fun leaveTypeParameterScope() {
        typeParameterScopes.removeFirst()
    }

    @DescriptorBasedIr
    override fun resolveScopedTypeParameter(typeParameterDescriptor: TypeParameterDescriptor): IrTypeParameterSymbol? {
        //Just support accessor scoped symbols resolve in external declaration
        //there should be enough to process only parent typeparameters
        return typeParameterScopes.firstOrNull()?.let { parent ->
            parent.typeParameters.firstOrNull {
                it.descriptor == typeParameterDescriptor
            }?.symbol
        }
    }
}