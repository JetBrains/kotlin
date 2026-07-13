/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.psi.*

/**
 * A [KaDeclarationSymbol] for the given [KtDeclaration].
 *
 * There are more specific `symbol` endpoints, such as [KtNamedFunction.symbol] and [KtClassOrObject.classSymbol], which can be used
 * when more specific PSI elements are available.
 */
context(session: KaSession)
public val KtDeclaration.symbol: KaDeclarationSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaVariableSymbol] for the given [KtParameter].
 *
 * Unfortunately, [KtParameter] in PSI stands for many things, and not all of them are represented by a single type of symbol,
 * so this function does not work for all possible [KtParameter]s.
 *
 * If [KtParameter.isFunctionTypeParameter] is `true`, i.e. if the given [KtParameter] is used as a function type parameter,
 * it is not possible to create [KaValueParameterSymbol], hence an error will be raised.
 *
 * If [KtParameter.isLoopParameter] is `true`, i.e. if the given [KtParameter] is a loop variable in `for` expression, then the symbol is
 * [KaLocalVariableSymbol].
 *
 * If [KtParameter.isContextParameter] is `true`, i.e. if the given [KtParameter] is used as a context parameter, then the symbol is
 * [KaContextParameterSymbol].
 *
 * Otherwise, the symbol is [KaValueParameterSymbol].
 */
context(session: KaSession)
public val KtParameter.symbol: KaVariableSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaFunctionSymbol] for the given [KtNamedFunction].
 *
 * If [KtNamedFunction.getName] is `null`, the symbol is a [KaAnonymousFunctionSymbol], and otherwise a [KaNamedFunctionSymbol].
 */
context(session: KaSession)
public val KtNamedFunction.symbol: KaFunctionSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaConstructorSymbol] for the given [KtConstructor].
 */
context(session: KaSession)
public val KtConstructor<*>.symbol: KaConstructorSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaTypeParameterSymbol] for the given [KtTypeParameter].
 */
context(session: KaSession)
public val KtTypeParameter.symbol: KaTypeParameterSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaTypeAliasSymbol] for the given [KtTypeAlias].
 */
context(session: KaSession)
public val KtTypeAlias.symbol: KaTypeAliasSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaEnumEntrySymbol] for the given [KtEnumEntry].
 */
context(session: KaSession)
public val KtEnumEntry.symbol: KaEnumEntrySymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaAnonymousFunctionSymbol] for the given [KtFunctionLiteral].
 */
context(session: KaSession)
public val KtFunctionLiteral.symbol: KaAnonymousFunctionSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaVariableSymbol] for the given [KtProperty].
 *
 * The symbol is a [KaKotlinPropertySymbol] for non-local properties, and a [KaLocalVariableSymbol] for local ones.
 */
context(session: KaSession)
public val KtProperty.symbol: KaVariableSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaBackingFieldSymbol] for the given [KtBackingField].
 */
context(session: KaSession)
public val KtBackingField.symbol: KaBackingFieldSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaAnonymousObjectSymbol] for the given [KtObjectLiteralExpression].
 */
context(session: KaSession)
public val KtObjectLiteralExpression.symbol: KaAnonymousObjectSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaClassSymbol] for the given [KtObjectDeclaration].
 *
 * The symbol may either be a [KaAnonymousObjectSymbol] if the given declaration is an [object expression](https://kotlinlang.org/docs/object-declarations.html#object-expressions),
 * or a [KaNamedClassSymbol] if it is a named object declaration.
 */
context(session: KaSession)
public val KtObjectDeclaration.symbol: KaClassSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaClassSymbol] for the given [KtClassOrObject], or `null` for [KtEnumEntry] declarations.
 *
 * To retrieve a [KaEnumEntrySymbol], please refer to [KtEnumEntry.symbol].
 */
context(session: KaSession)
public val KtClassOrObject.classSymbol: KaClassSymbol?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.classSymbol(this)
    }

/**
 * A [KaNamedClassSymbol] for the given named [KtClassOrObject], or `null` for [KtEnumEntry] declarations and object literals.
 */
context(session: KaSession)
public val KtClassOrObject.namedClassSymbol: KaNamedClassSymbol?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.namedClassSymbol(this)
    }

/**
 * A [KaPropertyAccessorSymbol] for the given [KtPropertyAccessor].
 */
context(session: KaSession)
public val KtPropertyAccessor.symbol: KaPropertyAccessorSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaClassInitializerSymbol] for the given [KtClassInitializer].
 */
context(session: KaSession)
public val KtClassInitializer.symbol: KaClassInitializerSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaVariableSymbol] that corresponds to the local variable introduced by the given [KtDestructuringDeclarationEntry].
 *
 * The symbol is usually a [KaLocalVariableSymbol]. However, for a top-level destructuring declaration in a script, the symbol is a
 * [KaKotlinPropertySymbol].
 *
 * #### Example
 *
 * ```kotlin
 * val (x, y) = p
 * ```
 *
 * The destructuring declaration above has two entries, one corresponding to `x` and another to `y`. For both of these entries, we can
 * retrieve a [KaVariableSymbol] which describes the entry.
 */
context(session: KaSession)
public val KtDestructuringDeclarationEntry.symbol: KaVariableSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaDestructuringDeclarationSymbol] for the given [KtDestructuringDeclaration].
 */
context(session: KaSession)
public val KtDestructuringDeclaration.symbol: KaDestructuringDeclarationSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaFileSymbol] for a [KtFile].
 */
context(session: KaSession)
public val KtFile.symbol: KaFileSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * A [KaScriptSymbol] for a [KtScript].
 */
context(session: KaSession)
public val KtScript.symbol: KaScriptSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }

/**
 * Represents [KtContextReceiver] as a [KaContextParameterSymbol].
 *
 * This is a temporary API for simplicity during the transition from context receivers to context parameters.
 *
 * **Note**: context receivers inside [KtFunctionType] are not supported.
 */
@KaExperimentalApi
context(session: KaSession)
public val KtContextReceiver.symbol: KaContextParameterSymbol
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.symbolProvider.symbol(this)
    }
