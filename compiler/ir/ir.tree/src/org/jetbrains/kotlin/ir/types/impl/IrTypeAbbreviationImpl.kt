/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types.impl

import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument

class IrTypeAbbreviationImpl(
    override val typeAlias: IrTypeAliasSymbol,
    override val hasQuestionMark: Boolean,
    override val arguments: List<IrTypeArgument>,
    override val annotations: List<IrConstructorCall>
) : IrTypeAbbreviation

class IrTypeAbbreviationBuilder {
    var typeAlias: IrTypeAliasSymbol? = null
    var hasQuestionMark: Boolean = false
    var arguments: List<IrTypeArgument> = emptyList()
    var annotations: List<IrConstructorCall> = emptyList()
}

fun IrTypeAbbreviationImpl.toBuilder() =
    IrTypeAbbreviationBuilder().also { b ->
        b.typeAlias = typeAlias
        b.hasQuestionMark = hasQuestionMark
        b.arguments = arguments
        b.annotations = annotations
    }

fun IrTypeAbbreviationBuilder.build() =
    IrTypeAbbreviationImpl(
        typeAlias ?: throw AssertionError("typeAlias not provided"),
        hasQuestionMark, arguments, annotations
    )