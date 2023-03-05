/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrSyntheticBodyImpl

abstract class IrBodyBuilder<Body : IrBody> @PublishedApi internal constructor(buildingContext: IrBuildingContext) :
    IrElementBuilder<Body>(buildingContext) {

}

class IrBlockBodyBuilder @PublishedApi internal constructor(buildingContext: IrBuildingContext) :
    IrBodyBuilder<IrBlockBody>(buildingContext), IrStatementContainerBuilder {

    override val __internal_statementBuilders = mutableListOf<IrStatementBuilder<*>>()

    @PublishedApi
    override fun build(): IrBlockBody {
        TODO("Not yet implemented")
    }
}

class IrExpressionBodyBuilder @PublishedApi internal constructor(buildingContext: IrBuildingContext) :
    IrBodyBuilder<IrExpressionBody>(buildingContext) {

    @PublishedApi
    override fun build(): IrExpressionBody {
        TODO("Not yet implemented")
    }
}

class IrSyntheticBodyBuilder @PublishedApi internal constructor(private val kind: IrSyntheticBodyKind, buildingContext: IrBuildingContext) :
    IrBodyBuilder<IrSyntheticBody>(buildingContext) {

    @PublishedApi
    override fun build(): IrSyntheticBody = IrSyntheticBodyImpl(startOffset, endOffset, kind)
}
