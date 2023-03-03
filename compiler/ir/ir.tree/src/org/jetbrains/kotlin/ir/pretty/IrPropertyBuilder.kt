/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrProperty

class IrPropertyBuilder @PublishedApi internal constructor(private val name: String, buildingContext: IrBuildingContext) :
    IrDeclarationBuilder<IrProperty>(buildingContext) {

    @PublishedApi
    override fun build(): IrProperty {
        TODO("Not yet implemented")
    }
}
