/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.components.KaSessionMixIn
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

public abstract class KaSymbolProvider : KaSessionComponent() {
    public open fun getSymbol(psi: KtDeclaration): KaDeclarationSymbol = when (psi) {
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
        is KtScriptInitializer -> getScriptSymbol(psi.containingDeclaration)
        is KtDestructuringDeclaration -> getDestructuringDeclarationSymbol(psi)
        else -> error("Cannot build symbol for ${psi::class}")
    }

    public abstract fun getParameterSymbol(psi: KtParameter): KaVariableLikeSymbol
    public abstract fun getFileSymbol(psi: KtFile): KaFileSymbol
    public abstract fun getScriptSymbol(psi: KtScript): KaScriptSymbol
    public abstract fun getFunctionLikeSymbol(psi: KtNamedFunction): KaFunctionLikeSymbol
    public abstract fun getConstructorSymbol(psi: KtConstructor<*>): KaConstructorSymbol
    public abstract fun getTypeParameterSymbol(psi: KtTypeParameter): KaTypeParameterSymbol
    public abstract fun getTypeAliasSymbol(psi: KtTypeAlias): KaTypeAliasSymbol
    public abstract fun getEnumEntrySymbol(psi: KtEnumEntry): KaEnumEntrySymbol
    public abstract fun getAnonymousFunctionSymbol(psi: KtNamedFunction): KaAnonymousFunctionSymbol
    public abstract fun getAnonymousFunctionSymbol(psi: KtFunctionLiteral): KaAnonymousFunctionSymbol
    public abstract fun getVariableSymbol(psi: KtProperty): KaVariableSymbol
    public abstract fun getAnonymousObjectSymbol(psi: KtObjectLiteralExpression): KaAnonymousObjectSymbol
    public abstract fun getClassOrObjectSymbol(psi: KtClassOrObject): KaClassOrObjectSymbol?
    public abstract fun getNamedClassOrObjectSymbol(psi: KtClassOrObject): KaNamedClassOrObjectSymbol?
    public abstract fun getPropertyAccessorSymbol(psi: KtPropertyAccessor): KaPropertyAccessorSymbol
    public abstract fun getClassInitializerSymbol(psi: KtClassInitializer): KaClassInitializerSymbol
    public abstract fun getDestructuringDeclarationEntrySymbol(psi: KtDestructuringDeclarationEntry): KaVariableSymbol
    public abstract fun getDestructuringDeclarationSymbol(psi: KtDestructuringDeclaration): KaDestructuringDeclarationSymbol

    public abstract fun getPackageSymbolIfPackageExists(packageFqName: FqName): KaPackageSymbol?

    public abstract fun getClassOrObjectSymbolByClassId(classId: ClassId): KaClassOrObjectSymbol?

    public abstract fun getTypeAliasByClassId(classId: ClassId): KaTypeAliasSymbol?

    public abstract fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol>

    @Suppress("PropertyName")
    public abstract val ROOT_PACKAGE_SYMBOL: KaPackageSymbol
}

public typealias KtSymbolProvider = KaSymbolProvider

public interface KaSymbolProviderMixIn : KaSessionMixIn {
    public fun KtDeclaration.getSymbol(): KaDeclarationSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getSymbol(this) }

    /**
     * Creates [KaVariableLikeSymbol] by [KtParameter].
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
    public fun KtParameter.getParameterSymbol(): KaVariableLikeSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getParameterSymbol(this) }

    /**
     * Creates [KaFunctionLikeSymbol] by [KtNamedFunction]
     *
     * If [KtNamedFunction.getName] is `null` then returns [KaAnonymousFunctionSymbol]
     * Otherwise, returns [KaFunctionSymbol]
     */
    public fun KtNamedFunction.getFunctionLikeSymbol(): KaFunctionLikeSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getFunctionLikeSymbol(this) }

    public fun KtConstructor<*>.getConstructorSymbol(): KaConstructorSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getConstructorSymbol(this) }

    public fun KtTypeParameter.getTypeParameterSymbol(): KaTypeParameterSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getTypeParameterSymbol(this) }

    public fun KtTypeAlias.getTypeAliasSymbol(): KaTypeAliasSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getTypeAliasSymbol(this) }

    public fun KtEnumEntry.getEnumEntrySymbol(): KaEnumEntrySymbol =
        withValidityAssertion { analysisSession.symbolProvider.getEnumEntrySymbol(this) }

    public fun KtNamedFunction.getAnonymousFunctionSymbol(): KaAnonymousFunctionSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getAnonymousFunctionSymbol(this) }

    public fun KtFunctionLiteral.getAnonymousFunctionSymbol(): KaAnonymousFunctionSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getAnonymousFunctionSymbol(this) }

    public fun KtProperty.getVariableSymbol(): KaVariableSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getVariableSymbol(this) }

    public fun KtObjectLiteralExpression.getAnonymousObjectSymbol(): KaAnonymousObjectSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getAnonymousObjectSymbol(this) }

    /** Returns a symbol for a given [KtClassOrObject]. Returns `null` for `KtEnumEntry` declarations. */
    public fun KtClassOrObject.getClassOrObjectSymbol(): KaClassOrObjectSymbol? =
        withValidityAssertion { analysisSession.symbolProvider.getClassOrObjectSymbol(this) }

    /** Returns a symbol for a given named [KtClassOrObject]. Returns `null` for `KtEnumEntry` declarations and object literals. */
    public fun KtClassOrObject.getNamedClassOrObjectSymbol(): KaNamedClassOrObjectSymbol? =
        withValidityAssertion { analysisSession.symbolProvider.getNamedClassOrObjectSymbol(this) }

    public fun KtPropertyAccessor.getPropertyAccessorSymbol(): KaPropertyAccessorSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getPropertyAccessorSymbol(this) }

    public fun KtFile.getFileSymbol(): KaFileSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getFileSymbol(this) }

    public fun KtScript.getScriptSymbol(): KaScriptSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getScriptSymbol(this) }

    /**
     * Returns [KaPackageSymbol] corresponding to [packageFqName] if corresponding package is exists and visible from current uses-site scope,
     * `null` otherwise
     */
    public fun getPackageSymbolIfPackageExists(packageFqName: FqName): KaPackageSymbol? =
        withValidityAssertion { analysisSession.symbolProvider.getPackageSymbolIfPackageExists(packageFqName) }

    /**
     * @return symbol with specified [this@getClassOrObjectSymbolByClassId] or `null` in case such symbol is not found
     */
    public fun getClassOrObjectSymbolByClassId(classId: ClassId): KaClassOrObjectSymbol? =
        withValidityAssertion { analysisSession.symbolProvider.getClassOrObjectSymbolByClassId(classId) }

    /**
     * @return [KaTypeAliasSymbol] with specified [classId] or `null` in case such symbol is not found
     */
    public fun getTypeAliasByClassId(classId: ClassId): KaTypeAliasSymbol? =
        withValidityAssertion { analysisSession.symbolProvider.getTypeAliasByClassId(classId) }

    /**
     * @return list of top-level functions and properties which are visible from current use-site module
     *
     * @param packageFqName package name in which callable symbols should be declared
     * @param name callable symbol name
     */
    public fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<KaCallableSymbol> =
        withValidityAssertion { analysisSession.symbolProvider.getTopLevelCallableSymbols(packageFqName, name) }

    /**
     * @return symbol corresponding to the local variable introduced by individual destructuring declaration entries.
     * E.g. `val (x, y) = p` has two declaration entries, one corresponding to `x`, one to `y`.
     */
    public fun KtDestructuringDeclarationEntry.getDestructuringDeclarationEntrySymbol(): KaVariableSymbol =
        analysisSession.symbolProvider.getDestructuringDeclarationEntrySymbol(this)

    @Suppress("PropertyName")
    public val ROOT_PACKAGE_SYMBOL: KaPackageSymbol
        get() = withValidityAssertion { analysisSession.symbolProvider.ROOT_PACKAGE_SYMBOL }
}

public typealias KtSymbolProviderMixIn = KaSymbolProviderMixIn

context(KaSession)
@Deprecated("Use 'getSymbol()' instead", ReplaceWith("this.getSymbol() as S"))
public inline fun <reified S : KaSymbol> KtDeclaration.getSymbolOfType(): S =
    withValidityAssertion { getSymbol() } as S

context(KaSession)
@Deprecated("Use 'getSymbol()' instead", ReplaceWith("this.getSymbol() as? S"))
public inline fun <reified S : KaSymbol> KtDeclaration.getSymbolOfTypeSafe(): S? =
    withValidityAssertion { getSymbol() } as? S



