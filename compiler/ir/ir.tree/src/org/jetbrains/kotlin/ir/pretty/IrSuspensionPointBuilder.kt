/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint

class IrSuspensionPointBuilder @PublishedApi internal constructor(buildingContext: IrBuildingContext) :
    IrExpressionBuilder<IrSuspensionPoint>(buildingContext) {

    @PublishedApi
    override fun build(): IrSuspensionPoint {
        TODO("Not yet implemented")
    }
}
