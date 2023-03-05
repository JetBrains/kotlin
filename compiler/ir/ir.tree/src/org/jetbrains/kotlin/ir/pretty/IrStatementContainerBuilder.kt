/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer

interface IrStatementContainerBuilder : IrDeclarationContainerBuilder {

    @Suppress("PropertyName") // TODO: Make @RequiresOptIn?
    val __internal_statementBuilders: MutableList<IrStatementBuilder<*>>

    @Suppress("FunctionName") // TODO: Make @RequiresOptIn?
    fun __internal_addStatementBuilder(statementBuilder: IrStatementBuilder<*>) {
        __internal_statementBuilders.add(statementBuilder)
    }

    override val __internal_declarationBuilders: MutableList<IrDeclarationBuilder<*>>
        get() = throw UnsupportedOperationException(
            "Attempt to read property ${::__internal_declarationBuilders.name} from IrStatementContainerBuilder. " +
                    "Read ${::__internal_statementBuilders.name} instead."
        )

    override fun __internal_addDeclarationBuilder(declarationBuilder: IrDeclarationBuilder<*>) {
        __internal_addStatementBuilder(declarationBuilder)
    }
}

@IrNodeBuilderDsl
inline fun IrStatementContainerBuilder.irBlock(block: IrElementBuilderClosure<IrBlockBuilder>) {
    __internal_addStatementBuilder(IrBlockBuilder(buildingContext).apply(block))
}

@Deprecated(
    "Use addStatementsTo instead of this for IrStatementContainerBuilder",
    ReplaceWith("addStatementsTo(declarationContainer)"),
    DeprecationLevel.ERROR
)
@Suppress("UnusedReceiverParameter")
internal fun IrStatementContainerBuilder.addDeclarationsTo(declarationContainer: IrDeclarationContainer) {
    throw UnsupportedOperationException("Use addStatementsTo instead of this for IrStatementContainerBuilder")
}

internal fun IrStatementContainerBuilder.addStatementsTo(statementContainer: IrStatementContainer) {
    for (statementBuilder in __internal_statementBuilders) {
        val statement = statementBuilder.build()
        // TODO: If statement is also a declaration, set its parent
        statementContainer.statements.add(statement)
    }
}
