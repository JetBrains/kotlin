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
            symbol.signature?.let { sig -> IrClassPublicSymbolImpl(sig) } ?: IrClassSymbolImpl()
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        remapSymbol(constructors, declaration) { symbol ->
            symbol.signature?.let { sig -> IrConstructorPublicSymbolImpl(sig) } ?: IrConstructorSymbolImpl()
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitEnumEntry(declaration: IrEnumEntry) {
        remapSymbol(enumEntries, declaration) { symbol ->
            symbol.signature?.let { sig -> IrEnumEntryPublicSymbolImpl(sig) } ?: IrEnumEntrySymbolImpl()
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        remapSymbol(functions, declaration) { symbol ->
            symbol.signature?.let { sig -> IrSimpleFunctionPublicSymbolImpl(sig) } ?: IrSimpleFunctionSymbolImpl()
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        remapSymbol(properties, declaration) { symbol ->
            symbol.signature?.let { sig -> IrPropertyPublicSymbolImpl(sig) } ?: IrPropertySymbolImpl()
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        remapSymbol(typeAliases, declaration) { symbol ->
            symbol.signature?.let { sig -> IrTypeAliasPublicSymbolImpl(sig) } ?: IrTypeAliasSymbolImpl()
        }
        declaration.acceptChildrenVoid(this)
    }
}