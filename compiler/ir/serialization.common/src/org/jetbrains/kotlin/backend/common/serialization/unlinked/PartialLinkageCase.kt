/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol

/**
 * Describes a reason why an [IrDeclaration] or an [IrExpression] is partially linked. Subclasses represent various causes of the p.l.
 */
internal sealed interface PartialLinkageCase {
    /**
     * There is no real owner declaration for the symbol, only synthetic stub created by [MissingDeclarationStubGenerator].
     * Likely the declaration has been deleted in newer version of the library.
     *
     * Applicable to: Declarations.
     */
    class MissingDeclaration(val missingDeclarationSymbol: IrSymbol) : PartialLinkageCase

    /**
     * There is no enclosing class for an inner class (or an enum entry). This may happen if the inner class became a top-level class
     * in newer version of the library.
     *
     * Applicable to: Declarations (classes).
     */
    class MissingEnclosingClass(val orphanedClassSymbol: IrClassSymbol) : PartialLinkageCase

    /**
     * Declaration's signature uses partially linked classifier symbol.
     *
     * Applicable to: Declarations.
     */
    class DeclarationUsesPartiallyLinkedClassifier(
        val declarationSymbol: IrSymbol,
        val cause: LinkedClassifierStatus.Partially
    ) : PartialLinkageCase

    /**
     * Unimplemented abstract callable member in non-abstract class.
     *
     * Applicable to: Declarations (functions, properties).
     */
    class UnimplementedAbstractCallable(val callable: IrOverridableDeclaration<*>) : PartialLinkageCase

    /**
     * Expression references a missing IR declaration (IR declaration)
     * Example: An [IrCall] references unlinked [IrSimpleFunctionSymbol].
     *
     * Applicable to: Expressions.
     */
    class ExpressionUsesMissingDeclaration(
        val expression: IrExpression,
        val missingDeclarationSymbol: IrSymbol
    ) : PartialLinkageCase

    /**
     * Expression uses partially linked classifier symbol.
     * Example: An [IrTypeOperatorCall] that casts an argument to a type with unlinked symbol.
     *
     * Applicable to: Expressions.
     */
    class ExpressionUsesPartiallyLinkedClassifier(
        val expression: IrExpression,
        val cause: LinkedClassifierStatus.Partially
    ) : PartialLinkageCase

    /**
     * Expression refers an IR declaration with a signature that uses partially linked classifier symbol
     *
     * Applicable to: Expressions.
     */
    class ExpressionUsesDeclarationThatUsesPartiallyLinkedClassifier(
        val expression: IrExpression,
        val referencedDeclarationSymbol: IrSymbol,
        val cause: LinkedClassifierStatus.Partially
    ) : PartialLinkageCase

    /**
     * Expression refers an IR declaration with the wrong type.
     * Example: An [IrEnumConstructorCall] that refers an [IrConstructor] of a regular class.
     *
     * Applicable to: Expressions.
     */
    class ExpressionUsesWrongTypeOfDeclaration(
        val expression: IrExpression,
        val actualDeclarationSymbol: IrSymbol,
        val expectedDeclarationDescription: String
    ) : PartialLinkageCase
}
