/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.components.KtAnalysisSessionMixIn
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

public abstract class KtSymbolProvider : KtAnalysisSessionComponent() {
    public open fun getSymbol(psi: KtDeclaration): KtDeclarationSymbol = when (psi) {
        is KtParameter -> getParameterSymbol(psi)
        is KtNamedFunction -> getFunctionLikeSymbol(psi)
        is KtConstructor<*> -> getConstructorSymbol(psi)
        is KtTypeParameter -> getTypeParameterSymbol(psi)
        is KtTypeAlias -> getTypeAliasSymbol(psi)
        is KtEnumEntry -> getEnumEntrySymbol(psi)
        is KtFunctionLiteral -> getAnonymousFunctionSymbol(psi)
        is KtProperty -> getVariableSymbol(psi)
        is KtClassOrObject -> {
            val literalExpression = (psi as? KtObjectDeclaration)?.parent as? KtObjectLiteralExpression
            literalExpression?.let(::getAnonymousObjectSymbol) ?: getClassOrObjectSymbol(psi)!!
        }
        is KtPropertyAccessor -> getPropertyAccessorSymbol(psi)
        is KtClassInitializer -> getClassInitializerSymbol(psi)
        is KtDestructuringDeclarationEntry -> getDestructuringDeclarationEntrySymbol(psi)
        is KtScript -> getScriptSymbol(psi)
        is KtDestructuringDeclaration -> getDestructuringDeclarationSymbol(psi)
        else -> error("Cannot build symbol for ${psi::class}")
    }

    public abstract fun getParameterSymbol(psi: KtParameter): KtVariableLikeSymbol
    public abstract fun getFileSymbol(psi: KtFile): KtFileSymbol
    public abstract fun getScriptSymbol(psi: KtScript): KtScriptSymbol
    public abstract fun getFunctionLikeSymbol(psi: KtNamedFunction): KtFunctionLikeSymbol
    public abstract fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol
    public abstract fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol
    public abstract fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol
    public abstract fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol
    public abstract fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol
    public abstract fun getAnonymousFunctionSymbol(psi: KtFunctionLiteral): KtAnonymousFunctionSymbol
    public abstract fun getVariableSymbol(psi: KtProperty): KtVariableSymbol
    public abstract fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KtAnonymousObjectSymbol
    public abstract fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol?
    public abstract fun getNamedClassOrObjectSymbol(psi: KtClassOrObject): KtNamedClassOrObjectSymbol?
    public abstract fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol
    public abstract fun getClassInitializerSymbol(psi: KtClassInitializer): KtClassInitializerSymbol
    public abstract fun getDestructuringDeclarationEntrySymbol(psi: KtDestructuringDeclarationEntry): KtLocalVariableSymbol
    public abstract fun getDestructuringDeclarationSymbol(psi: KtDestructuringDeclaration): KtDestructuringDeclarationSymbol

    public abstract fun getPackageSymbolIfPackageExists(packageFqName: FqName): KtPackageSymbol?

    public abstract fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol?

    public abstract fun getTypeAliasByClassId(classId: ClassId): KtTypeAliasSymbol?

    public abstract fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtCallableSymbol>

    @Suppress("PropertyName")
    public abstract val ROOT_PACKAGE_SYMBOL: KtPackageSymbol
}

public interface KtSymbolProviderMixIn : KtAnalysisSessionMixIn {
    public fun KtDeclaration.getSymbol(): KtDeclarationSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getSymbol(this) }

    /**
     * Creates [KtVariableLikeSymbol] by [KtParameter].
     *
     * Unfortunately, [KtParameter] in PSI stands for many things, and not all of them are represented by a single type of symbol,
     * so this function does not work for all possible [KtParameter]s.
     *
     * If [KtParameter.isFunctionTypeParameter] is `true`, i.e., if the given [KtParameter] is used as a function type parameter,
     * it is not possible to create [KtValueParameterSymbol], hence an error will be raised.
     *
     * If [KtParameter.isLoopParameter] is `true`, i.e. if the given [KtParameter] is a loop variable in `for` expression, then the function
     * returns [KtLocalVariableSymbol].
     *
     * Otherwise, returns [KtValueParameterSymbol].
     */
    public fun KtParameter.getParameterSymbol(): KtVariableLikeSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getParameterSymbol(this) }

    /**
     * Creates [KtFunctionLikeSymbol] by [KtNamedFunction]
     *
     * If [KtNamedFunction.getName] is `null` then returns [KtAnonymousFunctionSymbol]
     * Otherwise, returns [KtFunctionSymbol]
     */
    public fun KtNamedFunction.getFunctionLikeSymbol(): KtFunctionLikeSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getFunctionLikeSymbol(this) }

    public fun KtConstructor<*>.getConstructorSymbol(): KtConstructorSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getConstructorSymbol(this) }

    public fun KtTypeParameter.getTypeParameterSymbol(): KtTypeParameterSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getTypeParameterSymbol(this) }

    public fun KtTypeAlias.getTypeAliasSymbol(): KtTypeAliasSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getTypeAliasSymbol(this) }

    public fun KtEnumEntry.getEnumEntrySymbol(): KtEnumEntrySymbol =
        withValidityAssertion { analysisSession.symbolProvider.getEnumEntrySymbol(this) }

    public fun KtNamedFunction.getAnonymousFunctionSymbol(): KtAnonymousFunctionSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getAnonymousFunctionSymbol(this) }

    public fun KtFunctionLiteral.getAnonymousFunctionSymbol(): KtAnonymousFunctionSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getAnonymousFunctionSymbol(this) }

    public fun KtProperty.getVariableSymbol(): KtVariableSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getVariableSymbol(this) }

    public fun KtObjectLiteralExpression.getAnonymousObjectSymbol(): KtAnonymousObjectSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getAnonymousObjectSymbol(this) }

    /** Returns a symbol for a given [KtClassOrObject]. Returns `null` for `KtEnumEntry` declarations. */
    public fun KtClassOrObject.getClassOrObjectSymbol(): KtClassOrObjectSymbol? =
        withValidityAssertion { analysisSession.symbolProvider.getClassOrObjectSymbol(this) }

    /** Returns a symbol for a given named [KtClassOrObject]. Returns `null` for `KtEnumEntry` declarations and object literals. */
    public fun KtClassOrObject.getNamedClassOrObjectSymbol(): KtNamedClassOrObjectSymbol? =
        withValidityAssertion { analysisSession.symbolProvider.getNamedClassOrObjectSymbol(this) }

    public fun KtPropertyAccessor.getPropertyAccessorSymbol(): KtPropertyAccessorSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getPropertyAccessorSymbol(this) }

    public fun KtFile.getFileSymbol(): KtFileSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getFileSymbol(this) }

    public fun KtScript.getScriptSymbol(): KtScriptSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getScriptSymbol(this) }

    /**
     * Returns [KtPackageSymbol] corresponding to [packageFqName] if corresponding package is exists and visible from current uses-site scope,
     * `null` otherwise
     */
    public fun getPackageSymbolIfPackageExists(packageFqName: FqName): KtPackageSymbol? =
        withValidityAssertion { analysisSession.symbolProvider.getPackageSymbolIfPackageExists(packageFqName) }

    /**
     * @return symbol with specified [this@getClassOrObjectSymbolByClassId] or `null` in case such symbol is not found
     */
    public fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol? =
        withValidityAssertion { analysisSession.symbolProvider.getClassOrObjectSymbolByClassId(classId) }

    /**
     * @return [KtTypeAliasSymbol] with specified [classId] or `null` in case such symbol is not found
     */
    public fun getTypeAliasByClassId(classId: ClassId): KtTypeAliasSymbol? =
        withValidityAssertion { analysisSession.symbolProvider.getTypeAliasByClassId(classId) }

    /**
     * @return list of top-level functions and properties which are visible from current use-site module
     *
     * @param packageFqName package name in which callable symbols should be declared
     * @param name callable symbol name
     */
    public fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtCallableSymbol> =
        withValidityAssertion { analysisSession.symbolProvider.getTopLevelCallableSymbols(packageFqName, name) }

    /**
     * @return symbol corresponding to the local variable introduced by individual destructuring declaration entries.
     * E.g. `val (x, y) = p` has two declaration entries, one corresponding to `x`, one to `y`.
     */
    public fun KtDestructuringDeclarationEntry.getDestructuringDeclarationEntrySymbol(): KtLocalVariableSymbol =
        analysisSession.symbolProvider.getDestructuringDeclarationEntrySymbol(this)

    @Suppress("PropertyName")
    public val ROOT_PACKAGE_SYMBOL: KtPackageSymbol
        get() = withValidityAssertion { analysisSession.symbolProvider.ROOT_PACKAGE_SYMBOL }
}

context(KtAnalysisSession)
public inline fun <reified S : KtSymbol> KtDeclaration.getSymbolOfType(): S =
    withValidityAssertion { getSymbol() } as S

context(KtAnalysisSession)
public inline fun <reified S : KtSymbol> KtDeclaration.getSymbolOfTypeSafe(): S? =
    withValidityAssertion { getSymbol() } as? S



