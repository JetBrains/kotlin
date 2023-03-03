/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrErrorDeclaration

class IrErrorDeclarationBuilder @PublishedApi internal constructor(buildingContext: IrBuildingContext) : IrDeclarationBuilder<IrErrorDeclaration>(buildingContext) {

    @PublishedApi
    override fun build(): IrErrorDeclaration = buildingContext.irFactory.createErrorDeclaration(startOffset, endOffset)
}
