/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.components.KtAnalysisSessionComponent
import org.jetbrains.kotlin.idea.frontend.api.components.KtAnalysisSessionMixIn
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

abstract class KtSymbolProvider : KtAnalysisSessionComponent() {
    open fun getSymbol(psi: KtDeclaration): KtSymbol = when (psi) {
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
        else -> error("Cannot build symbol for ${psi::class}")
    }

    abstract fun getParameterSymbol(psi: KtParameter): KtValueParameterSymbol
    abstract fun getFileSymbol(psi: KtFile): KtFileSymbol
    abstract fun getFunctionLikeSymbol(psi: KtNamedFunction): KtFunctionLikeSymbol
    abstract fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol
    abstract fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol
    abstract fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol
    abstract fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol
    abstract fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol
    abstract fun getAnonymousFunctionSymbol(psi: KtFunctionLiteral): KtAnonymousFunctionSymbol
    abstract fun getVariableSymbol(psi: KtProperty): KtVariableSymbol
    abstract fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KtAnonymousObjectSymbol
    abstract fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol
    abstract fun getNamedClassOrObjectSymbol(psi: KtClassOrObject): KtNamedClassOrObjectSymbol
    abstract fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol

    abstract fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol?

    abstract fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtSymbol>

    @Suppress("PropertyName")
    abstract val ROOT_PACKAGE_SYMBOL: KtPackageSymbol
}

interface KtSymbolProviderMixIn : KtAnalysisSessionMixIn {
    fun KtDeclaration.getSymbol(): KtSymbol =
        analysisSession.symbolProvider.getSymbol(this)

    fun KtParameter.getParameterSymbol(): KtValueParameterSymbol =
        analysisSession.symbolProvider.getParameterSymbol(this)

    /**
     * Creates [KtFunctionLikeSymbol] by [KtNamedFunction]
     *
     * If [KtNamedFunction.getName] is `null` then returns [KtAnonymousFunctionSymbol]
     * Otherwise, returns [KtFunctionSymbol]
     */
    fun KtNamedFunction.getFunctionLikeSymbol(): KtFunctionLikeSymbol =
        analysisSession.symbolProvider.getFunctionLikeSymbol(this)

    fun KtConstructor<*>.getConstructorSymbol(): KtConstructorSymbol =
        analysisSession.symbolProvider.getConstructorSymbol(this)

    fun KtTypeParameter.getTypeParameterSymbol(): KtTypeParameterSymbol =
        analysisSession.symbolProvider.getTypeParameterSymbol(this)

    fun KtTypeAlias.getTypeAliasSymbol(): KtTypeAliasSymbol =
        analysisSession.symbolProvider.getTypeAliasSymbol(this)

    fun KtEnumEntry.getEnumEntrySymbol(): KtEnumEntrySymbol =
        analysisSession.symbolProvider.getEnumEntrySymbol(this)

    fun KtNamedFunction.getAnonymousFunctionSymbol(): KtAnonymousFunctionSymbol =
        analysisSession.symbolProvider.getAnonymousFunctionSymbol(this)

    fun KtFunctionLiteral.getAnonymousFunctionSymbol(): KtAnonymousFunctionSymbol =
        analysisSession.symbolProvider.getAnonymousFunctionSymbol(this)

    fun KtProperty.getVariableSymbol(): KtVariableSymbol =
        analysisSession.symbolProvider.getVariableSymbol(this)

    fun KtObjectLiteralExpression.getAnonymousObjectSymbol(): KtAnonymousObjectSymbol =
        analysisSession.symbolProvider.getAnonymousObjectSymbol(this)

    fun KtClassOrObject.getClassOrObjectSymbol(): KtClassOrObjectSymbol =
        analysisSession.symbolProvider.getClassOrObjectSymbol(this)

    fun KtClassOrObject.getNamedClassOrObjectSymbol(): KtNamedClassOrObjectSymbol =
        analysisSession.symbolProvider.getNamedClassOrObjectSymbol(this)

    fun KtPropertyAccessor.getPropertyAccessorSymbol(): KtPropertyAccessorSymbol =
        analysisSession.symbolProvider.getPropertyAccessorSymbol(this)

    fun KtFile.getFileSymbol(): KtFileSymbol =
        analysisSession.symbolProvider.getFileSymbol(this)

    /**
     * @return symbol with specified [this@getClassOrObjectSymbolByClassId] or `null` in case such symbol is not found
     */
    fun ClassId.getCorrespondingToplevelClassOrObjectSymbol(): KtClassOrObjectSymbol? =
        analysisSession.symbolProvider.getClassOrObjectSymbolByClassId(this)

    fun FqName.getContainingCallableSymbolsWithName(name: Name): Sequence<KtSymbol> =
        analysisSession.symbolProvider.getTopLevelCallableSymbols(this, name)

    @Suppress("PropertyName")
    val ROOT_PACKAGE_SYMBOL: KtPackageSymbol
        get() = analysisSession.symbolProvider.ROOT_PACKAGE_SYMBOL
}