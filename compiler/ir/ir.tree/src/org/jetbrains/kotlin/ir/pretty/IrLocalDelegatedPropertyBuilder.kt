/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.name.Name

class IrLocalDelegatedPropertyBuilder @PublishedApi internal constructor(private val name: Name, buildingContext: IrBuildingContext) :
    IrDeclarationBuilder<IrLocalDelegatedProperty>(buildingContext) {

    @PublishedApi
    override fun build(): IrLocalDelegatedProperty {
        TODO("Not yet implemented")
    }
}
