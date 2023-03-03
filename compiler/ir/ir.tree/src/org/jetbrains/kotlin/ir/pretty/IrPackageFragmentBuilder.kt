/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.name.FqName

abstract class IrPackageFragmentBuilder<PackageFragment : IrPackageFragment>(buildingContext: IrBuildingContext) :
    IrElementBuilder<PackageFragment>(buildingContext),
    IrSymbolOwnerBuilder {

    protected var packageFqName: FqName by SetAtMostOnce(FqName(""))

    override var symbolReference: String? by SetAtMostOnce(null)

    @Deprecated(
        "Custom debug info is not supported for IrPackageFragment (including IrFile and IrExternalPackageFragment)",
        replaceWith = ReplaceWith(""),
        level = DeprecationLevel.ERROR,
    )
    override fun debugInfo(startOffset: Int, endOffset: Int) {
        throw UnsupportedOperationException(
            "Custom debug info is not supported for IrPackageFragment (including IrFile and IrExternalPackageFragment)"
        )
    }

    @PrettyIrDsl
    fun packageName(name: String) = packageName(FqName(name))

    @PrettyIrDsl
    fun packageName(name: FqName) {
        packageFqName = name
    }
}
