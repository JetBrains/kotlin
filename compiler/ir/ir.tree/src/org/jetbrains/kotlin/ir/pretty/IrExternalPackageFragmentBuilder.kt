/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl

class IrExternalPackageFragmentBuilder @PublishedApi internal constructor(buildingContext: IrBuildingContext) :
    IrPackageFragmentBuilder<IrExternalPackageFragment>(buildingContext) {

    @PublishedApi
    override fun build(): IrExternalPackageFragment {
        return IrExternalPackageFragmentImpl(
            symbol = symbol<IrExternalPackageFragmentSymbol>(::IrExternalPackageFragmentSymbolImpl),
            fqName = packageFqName
        ).also {
            recordSymbolFromOwner(it)
        }
    }
}
