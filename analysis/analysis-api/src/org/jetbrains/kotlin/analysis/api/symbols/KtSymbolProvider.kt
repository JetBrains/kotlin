/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.components.KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.components.KtAnalysisSessionMixIn
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

public abstract class KtSymbolProvider : KtAnalysisSessionComponent() {
    public open fun getSymbol(psi: KtDeclaration): KtSymbol = when (psi) {
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
            literalExpression?.let(::getAnonymousObjectSymbol) ?: getClassOrObjectSymbol(psi)
        }
        is KtPropertyAccessor -> getPropertyAccessorSymbol(psi)
        is KtClassInitializer -> getClassInitializerSymbol(psi)
        else -> error("Cannot build symbol for ${psi::class}")
    }

    public abstract fun getParameterSymbol(psi: KtParameter): KtVariableLikeSymbol
    public abstract fun getFileSymbol(psi: KtFile): KtFileSymbol
    public abstract fun getFunctionLikeSymbol(psi: KtNamedFunction): KtFunctionLikeSymbol
    public abstract fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol
    public abstract fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol
    public abstract fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol
    public abstract fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol
    public abstract fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol
    public abstract fun getAnonymousFunctionSymbol(psi: KtFunctionLiteral): KtAnonymousFunctionSymbol
    public abstract fun getVariableSymbol(psi: KtProperty): KtVariableSymbol
    public abstract fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KtAnonymousObjectSymbol
    public abstract fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol
    public abstract fun getNamedClassOrObjectSymbol(psi: KtClassOrObject): KtNamedClassOrObjectSymbol?
    public abstract fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol
    public abstract fun getClassInitializerSymbol(psi: KtClassInitializer): KtClassInitializerSymbol

    public abstract fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol?

    public abstract fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtSymbol>

    @Suppress("PropertyName")
    public abstract val ROOT_PACKAGE_SYMBOL: KtPackageSymbol
}

public interface KtSymbolProviderMixIn : KtAnalysisSessionMixIn {
    public fun KtDeclaration.getSymbol(): KtSymbol =
        analysisSession.symbolProvider.getSymbol(this)

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
        analysisSession.symbolProvider.getParameterSymbol(this)

    /**
     * Creates [KtFunctionLikeSymbol] by [KtNamedFunction]
     *
     * If [KtNamedFunction.getName] is `null` then returns [KtAnonymousFunctionSymbol]
     * Otherwise, returns [KtFunctionSymbol]
     */
    public fun KtNamedFunction.getFunctionLikeSymbol(): KtFunctionLikeSymbol =
        analysisSession.symbolProvider.getFunctionLikeSymbol(this)

    public fun KtConstructor<*>.getConstructorSymbol(): KtConstructorSymbol =
        analysisSession.symbolProvider.getConstructorSymbol(this)

    public fun KtTypeParameter.getTypeParameterSymbol(): KtTypeParameterSymbol =
        analysisSession.symbolProvider.getTypeParameterSymbol(this)

    public fun KtTypeAlias.getTypeAliasSymbol(): KtTypeAliasSymbol =
        analysisSession.symbolProvider.getTypeAliasSymbol(this)

    public fun KtEnumEntry.getEnumEntrySymbol(): KtEnumEntrySymbol =
        analysisSession.symbolProvider.getEnumEntrySymbol(this)

    public fun KtNamedFunction.getAnonymousFunctionSymbol(): KtAnonymousFunctionSymbol =
        analysisSession.symbolProvider.getAnonymousFunctionSymbol(this)

    public fun KtFunctionLiteral.getAnonymousFunctionSymbol(): KtAnonymousFunctionSymbol =
        analysisSession.symbolProvider.getAnonymousFunctionSymbol(this)

    public fun KtProperty.getVariableSymbol(): KtVariableSymbol =
        analysisSession.symbolProvider.getVariableSymbol(this)

    public fun KtObjectLiteralExpression.getAnonymousObjectSymbol(): KtAnonymousObjectSymbol =
        analysisSession.symbolProvider.getAnonymousObjectSymbol(this)

    public fun KtClassOrObject.getClassOrObjectSymbol(): KtClassOrObjectSymbol =
        analysisSession.symbolProvider.getClassOrObjectSymbol(this)

    /** Gets the corresponding class or object symbol or null if the given [KtClassOrObject] is an enum entry. */
    public fun KtClassOrObject.getNamedClassOrObjectSymbol(): KtNamedClassOrObjectSymbol? =
        analysisSession.symbolProvider.getNamedClassOrObjectSymbol(this)

    public fun KtPropertyAccessor.getPropertyAccessorSymbol(): KtPropertyAccessorSymbol =
        analysisSession.symbolProvider.getPropertyAccessorSymbol(this)

    public fun KtFile.getFileSymbol(): KtFileSymbol =
        analysisSession.symbolProvider.getFileSymbol(this)

    /**
     * @return symbol with specified [this@getClassOrObjectSymbolByClassId] or `null` in case such symbol is not found
     */
    public fun ClassId.getCorrespondingToplevelClassOrObjectSymbol(): KtClassOrObjectSymbol? =
        analysisSession.symbolProvider.getClassOrObjectSymbolByClassId(this)

    public fun FqName.getContainingCallableSymbolsWithName(name: Name): Sequence<KtSymbol> =
        analysisSession.symbolProvider.getTopLevelCallableSymbols(this, name)

    @Suppress("PropertyName")
    public val ROOT_PACKAGE_SYMBOL: KtPackageSymbol
        get() = analysisSession.symbolProvider.ROOT_PACKAGE_SYMBOL
}
