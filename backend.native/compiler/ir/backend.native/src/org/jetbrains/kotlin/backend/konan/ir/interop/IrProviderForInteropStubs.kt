/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IrProvider

/**
 * Generates external IR declarations for descriptors from interop libraries.
 * [isSpecialInteropCase] allows to delegate providing of interop symbols to other providers.
 * For example, for CEnums we need to generate non-lazy IR.
 */
class IrProviderForInteropStubs(
        private val declarationStubGenerator: DeclarationStubGenerator,
        private val isSpecialInteropCase: (IrSymbol) -> Boolean
) : IrProvider {
    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? =
            when {
                symbol.isBound -> symbol.owner as IrDeclaration
                isSpecialInteropCase(symbol) -> null
                symbol.isPublicApi && symbol.signature.run { IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test() } ->
                    declarationStubGenerator.generateMemberStub(symbol.descriptor)
                else -> null
            }

}