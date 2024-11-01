/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

/**
 * Provides a mapping between a PSI element and the corresponding [KaSymbol].
 *
 * **Note**: symbols can be created only for elements which are a part of the current [KaSession]
 * ([KaAnalysisScopeProvider.canBeAnalysed][org.jetbrains.kotlin.analysis.api.components.KaAnalysisScopeProvider.canBeAnalysed]
 * is **true** for them).
 *
 * @see org.jetbrains.kotlin.analysis.api.components.KaAnalysisScopeProvider
 */
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

    /**
     * Returns [KaPackageSymbol] corresponding to [fqName] if corresponding package exists and visible from current uses-site scope,
     * `null` otherwise
     */
    public fun findPackage(fqName: FqName): KaPackageSymbol?

    /**
     * @return symbol with specified [classId] or `null` in case such a symbol is not found
     */
    public fun findClass(classId: ClassId): KaClassSymbol?

    /**
     * @return [KaTypeAliasSymbol] with specified [classId] or `null` in case such a symbol is not found
     */
    public fun findTypeAlias(classId: ClassId): KaTypeAliasSymbol?

    /**
     * @return [KaClassLikeSymbol] with specified [classId] or `null` in case such a symbol is not found
     *
     * @see findClass
     * @see findTypeAlias
     */
    public fun findClassLike(classId: ClassId): KaClassLikeSymbol?

    /**
     * @return list of top-level functions and properties which are visible from the current use-site module
     *
     * @param packageFqName package name in which callable symbols should be declared
     * @param name callable symbol name
     */
    public fun findTopLevelCallables(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol>

    public val rootPackageSymbol: KaPackageSymbol
}
