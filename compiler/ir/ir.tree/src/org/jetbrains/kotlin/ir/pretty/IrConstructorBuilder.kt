/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.name.Name

class IrConstructorBuilder @PublishedApi internal constructor(
    name: Name,
    buildingContext: IrBuildingContext
) : IrFunctionBuilder<IrConstructor>(buildingContext) {

    @PublishedApi
    override fun build(): IrConstructor {
        TODO("Not yet implemented")
    }
}
