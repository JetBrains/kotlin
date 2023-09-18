/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunctionWithLateBinding
import org.jetbrains.kotlin.ir.declarations.IrPropertyWithLateBinding
import org.jetbrains.kotlin.ir.overrides.FakeOverrideBuilderStrategy
import org.jetbrains.kotlin.ir.overrides.IrUnimplementedOverridesStrategy.ProcessAsFakeOverrides
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertyPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.ir.util.SymbolTable


internal class Fir2IrFakeOverrideStrategy(
    friendModules: Map<String, List<String>>,
    val symbolTable: SymbolTable,
    val mangler: KotlinMangler.IrMangler
) : FakeOverrideBuilderStrategy(
    friendModules = friendModules,
    unimplementedOverridesStrategy = ProcessAsFakeOverrides
) {
    private val publicIdSignatureComputer = PublicIdSignatureComputer(mangler)
    override fun linkFunctionFakeOverride(function: IrFunctionWithLateBinding, manglerCompatibleMode: Boolean) {
        with(mangler) {
            if (function.isExported(manglerCompatibleMode)) {
                val signature = publicIdSignatureComputer.composePublicIdSignature(function, manglerCompatibleMode)
                symbolTable.declareSimpleFunction(
                    signature,
                    { IrSimpleFunctionPublicSymbolImpl(signature, descriptor = null) },
                    { function.acquireSymbol(it) }
                )
            } else {
                function.acquireSymbol(IrSimpleFunctionSymbolImpl())
            }
        }
    }

    override fun linkPropertyFakeOverride(property: IrPropertyWithLateBinding, manglerCompatibleMode: Boolean) {
        with(mangler) {
            if (property.isExported(manglerCompatibleMode)) {
                // The signature composer expects getter/setter correspondingPropertySymbol to be set correctly.
                // But we don't have any property symbol for now. To work around this problem, we are creating a temporary one,
                // and throw it away after computing signature.
                val tempSymbol = IrPropertySymbolImpl(null).apply { bind(property) }
                property.getter?.let { it.correspondingPropertySymbol = tempSymbol }
                property.setter?.let { it.correspondingPropertySymbol = tempSymbol }
                val signature = publicIdSignatureComputer.composePublicIdSignature(property, manglerCompatibleMode)
                property.getter?.let { it.correspondingPropertySymbol = null }
                property.setter?.let { it.correspondingPropertySymbol = null }
                symbolTable.declareProperty(
                    signature,
                    { IrPropertyPublicSymbolImpl(signature, descriptor = null) },
                    { property.acquireSymbol(it) }
                )
            } else {
                property.acquireSymbol(IrPropertySymbolImpl())
            }
        }

        property.getter?.let {
            it.correspondingPropertySymbol = property.symbol
            linkFunctionFakeOverride(
                it as? IrFunctionWithLateBinding ?: error("Unexpected fake override getter: $it"),
                manglerCompatibleMode
            )
        }
        property.setter?.let {
            it.correspondingPropertySymbol = property.symbol
            linkFunctionFakeOverride(
                it as? IrFunctionWithLateBinding ?: error("Unexpected fake override setter: $it"),
                manglerCompatibleMode
            )
        }
    }

    override fun inFile(file: IrFile?, block: () -> Unit) {
        publicIdSignatureComputer.inFile(file?.symbol, block)
    }
}
