/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.components.KtAnalysisSessionComponent
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

abstract class KtSymbolProvider : KtAnalysisSessionComponent() {
    open fun getSymbol(psi: KtDeclaration): KtSymbol = when (psi) {
        is KtParameter -> getParameterSymbol(psi)
        is KtNamedFunction -> getFunctionSymbol(psi)
        is KtConstructor<*> -> getConstructorSymbol(psi)
        is KtTypeParameter -> getTypeParameterSymbol(psi)
        is KtTypeAlias -> getTypeAliasSymbol(psi)
        is KtEnumEntry -> getEnumEntrySymbol(psi)
        is KtLambdaExpression -> getAnonymousFunctionSymbol(psi)
        is KtProperty -> getVariableSymbol(psi)
        is KtClassOrObject -> {
            val literalExpression = (psi as? KtObjectDeclaration)?.parent as? KtObjectLiteralExpression
            literalExpression?.let(::getAnonymousObjectSymbol) ?: getClassOrObjectSymbol(psi)
        }
        is KtPropertyAccessor -> getPropertyAccessorSymbol(psi)
        else -> error("Cannot build symbol for ${psi::class}")
    }

    abstract fun getParameterSymbol(psi: KtParameter): KtParameterSymbol
    abstract fun getFunctionSymbol(psi: KtNamedFunction): KtFunctionSymbol
    abstract fun getConstructorSymbol(psi: KtConstructor<*>): KtConstructorSymbol
    abstract fun getTypeParameterSymbol(psi: KtTypeParameter): KtTypeParameterSymbol
    abstract fun getTypeAliasSymbol(psi: KtTypeAlias): KtTypeAliasSymbol
    abstract fun getEnumEntrySymbol(psi: KtEnumEntry): KtEnumEntrySymbol
    abstract fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KtAnonymousFunctionSymbol
    abstract fun getAnonymousFunctionSymbol(psi: KtLambdaExpression): KtAnonymousFunctionSymbol
    abstract fun getVariableSymbol(psi: KtProperty): KtVariableSymbol
    abstract fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KtAnonymousObjectSymbol
    abstract fun getClassOrObjectSymbol(psi: KtClassOrObject): KtClassOrObjectSymbol
    abstract fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KtPropertyAccessorSymbol

    /**
     * @return symbol with specified [classId] or `null` in case such symbol is not found
     */
    abstract fun getClassOrObjectSymbolByClassId(classId: ClassId): KtClassOrObjectSymbol?

    abstract fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KtSymbol>
}