/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponent
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

/**
 * [KaSymbolProvider] provides [KaSymbol]s for given PSI elements.
 *
 * **Important**: Symbols can be created only for elements which are a part of the current [KaSession][org.jetbrains.kotlin.analysis.api.KaSession].
 *
 * @see org.jetbrains.kotlin.analysis.api.KaSession
 */
public interface KaSymbolProvider : KaSessionComponent {
    /**
     * A [KaDeclarationSymbol] for the given [KtDeclaration].
     *
     * There are more specific `symbol` endpoints, such as [KtNamedFunction.symbol] and [KtClassOrObject.classSymbol], which can be used
     * when more specific PSI elements are available.
     */
    public val KtDeclaration.symbol: KaDeclarationSymbol

    /**
     * A [KaVariableSymbol] for the given [KtParameter].
     *
     * Unfortunately, [KtParameter] in PSI stands for many things, and not all of them are represented by a single type of symbol,
     * so this function does not work for all possible [KtParameter]s.
     *
     * If [KtParameter.isFunctionTypeParameter] is `true`, i.e. if the given [KtParameter] is used as a function type parameter,
     * it is not possible to create [KaValueParameterSymbol], hence an error will be raised.
     *
     * If [KtParameter.isLoopParameter] is `true`, i.e. if the given [KtParameter] is a loop variable in `for` expression, then the function
     * returns [KaLocalVariableSymbol].
     *
     * If [KtParameter.isContextParameter] is `true`, i.e. if the given [KtParameter] is used as a context parameter, then the function
     * returns [KaContextParameterSymbol].
     *
     * Otherwise, returns [KaValueParameterSymbol].
     */
    public val KtParameter.symbol: KaVariableSymbol

    /**
     * A [KaFunctionSymbol] for the given [KtNamedFunction].
     *
     * If [KtNamedFunction.getName] is `null`, the symbol is a [KaAnonymousFunctionSymbol], and otherwise a [KaNamedFunctionSymbol].
     */
    public val KtNamedFunction.symbol: KaFunctionSymbol

    /**
     * A [KaConstructorSymbol] for the given [KtConstructor].
     */
    public val KtConstructor<*>.symbol: KaConstructorSymbol

    /**
     * A [KaTypeParameterSymbol] for the given [KtTypeParameter].
     */
    public val KtTypeParameter.symbol: KaTypeParameterSymbol

    /**
     * A [KaTypeAliasSymbol] for the given [KtTypeAlias].
     */
    public val KtTypeAlias.symbol: KaTypeAliasSymbol

    /**
     * A [KaEnumEntrySymbol] for the given [KtEnumEntry].
     */
    public val KtEnumEntry.symbol: KaEnumEntrySymbol

    /**
     * A [KaAnonymousFunctionSymbol] for the given [KtFunctionLiteral].
     */
    public val KtFunctionLiteral.symbol: KaAnonymousFunctionSymbol

    /**
     * A [KaVariableSymbol] for the given [KtProperty].
     *
     * The symbol is a [KaKotlinPropertySymbol] for non-local properties, and a [KaLocalVariableSymbol] for local ones.
     */
    public val KtProperty.symbol: KaVariableSymbol

    /**
     * A [KaAnonymousObjectSymbol] for the given [KtObjectLiteralExpression].
     */
    public val KtObjectLiteralExpression.symbol: KaAnonymousObjectSymbol

    /**
     * A [KaClassSymbol] for the given [KtClassOrObject], or `null` for [KtEnumEntry] declarations.
     *
     * To retrieve a [KaEnumEntrySymbol], please refer to [KtEnumEntry.symbol].
     */
    public val KtClassOrObject.classSymbol: KaClassSymbol?

    /**
     * A [KaClassSymbol] for the given [KtObjectDeclaration].
     *
     * The symbol may either be a [KaAnonymousObjectSymbol] if the given declaration is an [object expression](https://kotlinlang.org/docs/object-declarations.html#object-expressions),
     * or a [KaNamedClassSymbol] if it is a named object declaration.
     */
    public val KtObjectDeclaration.symbol: KaClassSymbol

    /**
     * A [KaNamedClassSymbol] for the given named [KtClassOrObject], or `null` for [KtEnumEntry] declarations and object literals.
     */
    public val KtClassOrObject.namedClassSymbol: KaNamedClassSymbol?

    /**
     * A [KaPropertyAccessorSymbol] for the given [KtPropertyAccessor].
     */
    public val KtPropertyAccessor.symbol: KaPropertyAccessorSymbol

    /**
     * A [KaClassInitializerSymbol] for the given [KtClassInitializer].
     */
    public val KtClassInitializer.symbol: KaClassInitializerSymbol

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
    public val KtDestructuringDeclarationEntry.symbol: KaVariableSymbol

    /**
     * A [KaDestructuringDeclarationSymbol] for the given [KtDestructuringDeclaration].
     */
    public val KtDestructuringDeclaration.symbol: KaDestructuringDeclarationSymbol

    /**
     * A [KaFileSymbol] for a [KtFile].
     */
    public val KtFile.symbol: KaFileSymbol

    /**
     * A [KaScriptSymbol] for a [KtScript].
     */
    public val KtScript.symbol: KaScriptSymbol

    /**
     * Represents [KtContextReceiver] as a [KaContextParameterSymbol].
     *
     * This is a temporary API for simplicity during the transition from context receivers to context parameters.
     *
     * **Note**: context receivers inside [KtFunctionType] are not supported.
     */
    @KaExperimentalApi
    public val KtContextReceiver.symbol: KaContextParameterSymbol

    /**
     * Returns a [KaPackageSymbol] corresponding to the given [fqName] if that package exists and is visible from the current use site, or
     * `null` otherwise.
     */
    public fun findPackage(fqName: FqName): KaPackageSymbol?

    /**
     * Returns a [KaClassSymbol] for the specified [ClassId], or `null` if such a symbol cannot be found.
     */
    public fun findClass(classId: ClassId): KaClassSymbol?

    /**
     * Returns a [KaTypeAliasSymbol] for the specified [ClassId], or `null` if such a symbol cannot be found.
     */
    public fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol?

    /**
     * Returns a [KaClassLikeSymbol] for the specified [ClassId], or `null` if such a symbol cannot be found.
     *
     * The function combines both class search (see [findClass]) and type alias search (see [findTypeAlias]).
     */
    public fun findClassLike(classId: ClassId): KaClassLikeSymbol?

    /**
     * Finds top-level functions and properties called [name] in the package called [packageFqName]. Returns only symbols that are visible
     * from the current use-site module.
     */
    public fun findTopLevelCallables(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol>

    /**
     * A [KaPackageSymbol] for the *root package*, which is the special package with an empty fully-qualified name.
     */
    public val rootPackageSymbol: KaPackageSymbol
}
