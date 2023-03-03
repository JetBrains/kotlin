/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrValueParameter

class IrValueParameterBuilder @PublishedApi internal constructor(private val name: String, buildingContext: IrBuildingContext) :
    IrDeclarationBuilder<IrValueParameter>(buildingContext) {

    @PublishedApi
    override fun build(): IrValueParameter {
        TODO("Not yet implemented")
    }
}
