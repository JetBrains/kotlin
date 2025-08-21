/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*

/**
 * Describes a reason why an [IrDeclaration] or an [IrExpression] is partially linked. Subclasses represent various causes of the p.l.
 */
sealed interface PartialLinkageCase {
    /**
     * Unusable (partially linked) classifier.
     *
     * Applicable to: Declarations (classifiers).
     */
    class UnusableClassifier(val cause: ClassifierPartialLinkageStatus.Unusable.CanBeRootCause) : PartialLinkageCase

    /**
     * There is no real owner declaration for the symbol, only synthetic stub created by [MissingDeclarationStubGenerator].
     * Likely the declaration has been deleted in newer version of the library.
     *
     * Applicable to: Declarations.
     */
    class MissingDeclaration(val missingDeclarationSymbol: IrSymbol) : PartialLinkageCase

    /**
     * Declaration's signature uses an unusable (partially linked) classifier symbol.
     *
     * Applicable to: Declarations.
     */
    class DeclarationWithUnusableClassifier(
        val declarationSymbol: IrSymbol,
        val cause: ClassifierPartialLinkageStatus.Unusable
    ) : PartialLinkageCase

    /**
     * Expression uses an unusable (partially linked) classifier symbol.
     * Example: An [IrTypeOperatorCall] that casts an argument to a type with unlinked symbol.
     *
     * Applicable to: Expressions.
     */
    class ExpressionWithUnusableClassifier(
        val expression: IrExpression,
        val cause: ClassifierPartialLinkageStatus.Unusable
    ) : PartialLinkageCase

    /**
     * Expression references a missing IR declaration (IR declaration)
     * Example: An [IrCall] references unlinked [IrSimpleFunctionSymbol].
     *
     * Applicable to: Expressions.
     */
    class ExpressionWithMissingDeclaration(
        val expression: IrExpression,
        val missingDeclarationSymbol: IrSymbol
    ) : PartialLinkageCase

    /**
     * Expression refers an IR declaration with a signature that uses an unusable (partially linked) classifier symbol.
     *
     * Applicable to: Expressions.
     */
    class ExpressionHasDeclarationWithUnusableClassifier(
        val expression: IrExpression,
        val referencedDeclarationSymbol: IrSymbol,
        val cause: ClassifierPartialLinkageStatus.Unusable
    ) : PartialLinkageCase

    /**
     * Expression refers an IR declaration with the wrong type.
     * Example: An [IrEnumConstructorCall] that refers an [IrConstructor] of a regular class.
     *
     * Applicable to: Expressions.
     */
    class ExpressionHasWrongTypeOfDeclaration(
        val expression: IrExpression,
        val actualDeclarationSymbol: IrSymbol,
        val expectedDeclarationDescription: String
    ) : PartialLinkageCase

    /**
     * Expression whose value arguments do not match the number of value parameters in a function,
     * or if argument's value has not been provided and there is no default value either
     * (which may happen, for example, is a default value for a value parameter was removed).
     *
     * Applicable to: Expressions.
     */
    sealed class MemberAccessExpressionArgumentsMismatch(
        val expression: IrMemberAccessExpression<IrFunctionSymbol>,
    ) : PartialLinkageCase {
        class ExcessiveArguments(
            expression: IrMemberAccessExpression<IrFunctionSymbol>,
            val count: Int,
        ) : MemberAccessExpressionArgumentsMismatch(expression)

        class MissingArguments(
            expression: IrMemberAccessExpression<IrFunctionSymbol>,
            val forParameters: List<IrValueParameter>,
        ) : MemberAccessExpressionArgumentsMismatch(expression)

        class MissingArgumentValues(
            expression: IrMemberAccessExpression<IrFunctionSymbol>,
            val forParameters: List<IrValueParameter>,
        ) : MemberAccessExpressionArgumentsMismatch(expression)
    }

    /**
     * SAM-conversion to a function interface that effectively has more than one abstract function or at least one abstract property.
     *
     * Applicable to: Expressions.
     */
    sealed class InvalidSamConversion(
        val expression: IrExpression,
    ) : PartialLinkageCase {
        class NotAFunInterface(
            expression: IrExpression,
            val classifier: IrClassifierSymbol,
        ) : InvalidSamConversion(expression)

        class FunInterfaceHasNotSingleFunction(
            expression: IrExpression,
            val funInterface: IrClassifierSymbol,
            val abstractFunctionSymbols: Set<IrSimpleFunctionSymbol>,
        ) : InvalidSamConversion(expression)

        class FunInterfaceHasAbstractProperty(
            expression: IrExpression,
            val funInterface: IrClassifierSymbol,
            val abstractPropertySymbol: IrPropertySymbol
        ) : InvalidSamConversion(expression)

        class FunctionIsIncompatible(
            expression: IrExpression,
            val originalOverriddenFunction: IrSimpleFunctionSymbol,
            val newOverriddenFunction: IrSimpleFunctionSymbol,
        ) : InvalidSamConversion(expression)

        class SamChanged(
            expression: IrExpression,
            val funInterface: IrClassifierSymbol,
            val originalOverriddenFunction: IrSimpleFunctionSymbol,
            val newOverriddenFunction: IrSimpleFunctionSymbol,
        ) : InvalidSamConversion(expression)
    }

    /**
     * An [IrCall] of suspendable function at the place where no coroutine context is available.
     *
     * Applicable to: Expressions.
     */
    class SuspendableFunctionCallWithoutCoroutineContext(val expression: IrCall) : PartialLinkageCase

    /**
     * A non-local return in context where it is not expected.
     *
     * Applicable to: Expressions.
     */
    class IllegalNonLocalReturn(val expression: IrReturn, val validReturnTargets: Set<IrReturnTargetSymbol>) : PartialLinkageCase

    /**
     * Expression refers an IR declaration that is not accessible at the use site.
     * Example: An [IrCall] that refers a private [IrSimpleFunction] from another module.
     *
     * Applicable to: Expressions.
     */
    class ExpressionHasInaccessibleDeclaration(
        val expression: IrExpression,
        val referencedDeclarationSymbol: IrSymbol,
        val declaringModule: PartialLinkageSources.Module,
        val useSiteModule: PartialLinkageSources.Module
    ) : PartialLinkageCase

    /**
     * An [IrConstructor] delegates call to [unexpectedSuperClassConstructorSymbol] while should delegate to
     * one of constructors of [superClassSymbol].
     */
    class InvalidConstructorDelegation(
        val constructorSymbol: IrConstructorSymbol,
        val superClassSymbol: IrClassSymbol,
        val unexpectedSuperClassConstructorSymbol: IrConstructorSymbol
    ) : PartialLinkageCase

    /**
     * An attempt to instantiate an abstract class from outside its inheritance hierarchy.
     */
    class AbstractClassInstantiation(val constructorCall: IrConstructorCall, val classSymbol: IrClassSymbol) : PartialLinkageCase

    /**
     * Unimplemented abstract callable member in non-abstract class.
     *
     * Applicable to: Declarations (functions, properties).
     */
    class UnimplementedAbstractCallable(val callable: IrOverridableDeclaration<*>) : PartialLinkageCase

    /**
     * Unusable instance of annotation. The annotation itself is removed from the holder [IrDeclaration].
     *
     * Applicable to: Declarations.
     */
    class UnusableAnnotation(
        val annotationConstructorSymbol: IrConstructorSymbol,
        val holderDeclarationSymbol: IrSymbol
    ) : PartialLinkageCase

    /**
     * Callable, which is not implemented, but inherited several implementations from super interfaces.
     *
     * Applicable to: Declarations (functions, properties).
     */
    class AmbiguousNonOverriddenCallable(val callable: IrOverridableDeclaration<*>) : PartialLinkageCase
}