/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

public interface KaSymbolProvider {
    public val KtDeclaration.symbol: KaDeclarationSymbol

    /**
     * Creates [KaVariableSymbol] by [KtParameter].
     *
     * Unfortunately, [KtParameter] in PSI stands for many things, and not all of them are represented by a single type of symbol,
     * so this function does not work for all possible [KtParameter]s.
     *
     * If [KtParameter.isFunctionTypeParameter] is `true`, i.e., if the given [KtParameter] is used as a function type parameter,
     * it is not possible to create [KaValueParameterSymbol], hence an error will be raised.
     *
     * If [KtParameter.isLoopParameter] is `true`, i.e. if the given [KtParameter] is a loop variable in `for` expression, then the function
     * returns [KaLocalVariableSymbol].
     *
     * Otherwise, returns [KaValueParameterSymbol].
     */
    public val KtParameter.symbol: KaVariableSymbol

    /**
     * Creates [KaFunctionSymbol] by [KtNamedFunction]
     *
     * If [KtNamedFunction.getName] is `null` then returns [KaAnonymousFunctionSymbol]
     * Otherwise, returns [KaNamedFunctionSymbol]
     */
    public val KtNamedFunction.symbol: KaFunctionSymbol

    public val KtConstructor<*>.symbol: KaConstructorSymbol

    public val KtTypeParameter.symbol: KaTypeParameterSymbol

    public val KtTypeAlias.symbol: KaTypeAliasSymbol

    public val KtEnumEntry.symbol: KaEnumEntrySymbol

    @Deprecated("This API is unsafe. Use 'symbol' instead", replaceWith = ReplaceWith("symbol as KaAnonymousFunctionSymbol"))
    public val KtNamedFunction.anonymousSymbol: KaAnonymousFunctionSymbol
        get() = symbol as KaAnonymousFunctionSymbol

    public val KtFunctionLiteral.symbol: KaAnonymousFunctionSymbol

    public val KtProperty.symbol: KaVariableSymbol

    public val KtObjectLiteralExpression.symbol: KaAnonymousObjectSymbol

    /** Returns a symbol for a given [KtClassOrObject]. Returns `null` for [KtEnumEntry] declarations. */
    public val KtClassOrObject.classSymbol: KaClassSymbol?

    public val KtObjectDeclaration.symbol: KaClassSymbol

    /** Returns a symbol for a given named [KtClassOrObject]. Returns `null` for [KtEnumEntry] declarations and object literals. */
    public val KtClassOrObject.namedClassSymbol: KaNamedClassSymbol?

    public val KtPropertyAccessor.symbol: KaPropertyAccessorSymbol

    public val KtClassInitializer.symbol: KaClassInitializerSymbol

    /**
     * @return symbol corresponding to the local variable introduced by individual destructuring declaration entries.
     * E.g. `val (x, y) = p` has two declaration entries, one corresponding to `x`, one to `y`.
     */
    public val KtDestructuringDeclarationEntry.symbol: KaVariableSymbol

    public val KtDestructuringDeclaration.symbol: KaDestructuringDeclarationSymbol

    public val KtFile.symbol: KaFileSymbol

    public val KtScript.symbol: KaScriptSymbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtParameter.getParameterSymbol(): KaVariableSymbol = symbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtNamedFunction.getFunctionLikeSymbol(): KaFunctionSymbol = symbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtConstructor<*>.getConstructorSymbol(): KaConstructorSymbol = symbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtTypeParameter.getTypeParameterSymbol(): KaTypeParameterSymbol = symbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtTypeAlias.getTypeAliasSymbol(): KaTypeAliasSymbol = symbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtEnumEntry.getEnumEntrySymbol(): KaEnumEntrySymbol = symbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol as KaAnonymousFunctionSymbol"))
    public fun KtNamedFunction.getAnonymousFunctionSymbol(): KaAnonymousFunctionSymbol = symbol as KaAnonymousFunctionSymbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtFunctionLiteral.getAnonymousFunctionSymbol(): KaAnonymousFunctionSymbol = symbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtProperty.getVariableSymbol(): KaVariableSymbol = symbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtObjectLiteralExpression.getAnonymousObjectSymbol(): KaAnonymousObjectSymbol = symbol

    @Deprecated("Use 'classSymbol' instead", replaceWith = ReplaceWith("classSymbol"))
    public fun KtClassOrObject.getClassOrObjectSymbol(): KaClassSymbol? = classSymbol

    @Deprecated("Use 'namedClassSymbol' instead", replaceWith = ReplaceWith("namedClassSymbol"))
    public fun KtClassOrObject.getNamedClassOrObjectSymbol(): KaNamedClassSymbol? = namedClassSymbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtPropertyAccessor.getPropertyAccessorSymbol(): KaPropertyAccessorSymbol = symbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtFile.getFileSymbol(): KaFileSymbol = symbol

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtScript.getScriptSymbol(): KaScriptSymbol = symbol

    /**
     * Returns [KaPackageSymbol] corresponding to [fqName] if corresponding package exists and visible from current uses-site scope,
     * `null` otherwise
     */
    public fun findPackage(fqName: FqName): KaPackageSymbol?

    @Deprecated("Use 'findPackage()' instead.", replaceWith = ReplaceWith("findPackage(packageFqName)"))
    public fun getPackageSymbolIfPackageExists(packageFqName: FqName): KaPackageSymbol? = findPackage(packageFqName)

    /**
     * @return symbol with specified [classId] or `null` in case such a symbol is not found
     */
    public fun findClass(classId: ClassId): KaClassSymbol?

    @Deprecated("Use 'findClass() instead.", replaceWith = ReplaceWith("findClass(classId)"))
    public fun getClassOrObjectSymbolByClassId(classId: ClassId): KaClassSymbol? = findClass(classId)

    /**
     * @return [KaTypeAliasSymbol] with specified [classId] or `null` in case such a symbol is not found
     */
    public fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol?

    @Deprecated("Use 'findTypeAlias()' instead.", replaceWith = ReplaceWith("findTypeAlias(classId)"))
    public fun getTypeAliasByClassId(classId: ClassId): KaTypeAliasSymbol? = findTypeAlias(classId)

    /**
     * @return list of top-level functions and properties which are visible from the current use-site module
     *
     * @param packageFqName package name in which callable symbols should be declared
     * @param name callable symbol name
     */
    public fun findTopLevelCallables(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol>

    @Deprecated(
        "Use 'findTopLevelCallables()' instead.",
        replaceWith = ReplaceWith("findTopLevelCallables(packageFqName, name)")
    )
    public fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol> =
        findTopLevelCallables(packageFqName, name)

    @Deprecated("Use 'symbol' instead", replaceWith = ReplaceWith("symbol"))
    public fun KtDestructuringDeclarationEntry.getDestructuringDeclarationEntrySymbol(): KaVariableSymbol = symbol

    public val rootPackageSymbol: KaPackageSymbol

    @Suppress("PropertyName")
    @Deprecated("Use 'rootPackageSymbol' instead.", replaceWith = ReplaceWith("rootPackageSymbol"))
    public val ROOT_PACKAGE_SYMBOL: KaPackageSymbol
        get() = rootPackageSymbol
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
@Deprecated("Use 'getSymbol()' instead", ReplaceWith("this.getSymbol() as S"))
public inline fun <reified S : KaSymbol> KtDeclaration.getSymbolOfType(): S =
    withValidityAssertion { symbol } as S

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
@Deprecated("Use 'getSymbol()' instead", ReplaceWith("this.getSymbol() as? S"))
public inline fun <reified S : KaSymbol> KtDeclaration.getSymbolOfTypeSafe(): S? =
    withValidityAssertion { symbol } as? S