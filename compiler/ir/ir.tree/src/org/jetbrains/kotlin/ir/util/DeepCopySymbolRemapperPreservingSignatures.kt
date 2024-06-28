/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

open class DeepCopySymbolRemapperPreservingSignatures : DeepCopySymbolRemapper() {
    override fun visitClass(declaration: IrClass) {
        remapSymbol(classes, declaration) { symbol ->
            IrClassSymbolImpl(signature = symbol.signature)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        remapSymbol(constructors, declaration) { symbol ->
            IrConstructorSymbolImpl(signature = symbol.signature)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        remapSymbol(enumEntries, declaration) { symbol ->
            IrEnumEntrySymbolImpl(signature = symbol.signature)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        remapSymbol(functions, declaration) { symbol ->
            IrSimpleFunctionSymbolImpl(signature = symbol.signature)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        remapSymbol(properties, declaration) { symbol ->
            IrPropertySymbolImpl(signature = symbol.signature)
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        remapSymbol(typeAliases, declaration) { symbol ->
            IrTypeAliasSymbolImpl(signature = symbol.signature)
        }
        declaration.acceptChildrenVoid(this)
    }
}