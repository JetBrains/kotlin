/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.frontend.api.components.*
import org.jetbrains.kotlin.idea.frontend.api.scopes.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleReference
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

/**
 * The entry point into all frontend-related work. Has the following contracts:
 * - Should not be accessed from event dispatch thread
 * - Should not be accessed outside read action
 * - Should not be leaked outside read action it was created in
 * - To be sure that session is not leaked it is forbidden to store it in a variable, consider working with it only in [analyze] context
 * - All entities retrieved from analysis session should not be leaked outside the read action KtAnalysisSession was created in
 *
 * To pass a symbol from one read action to another use [KtSymbolPointer] which can be created from a symbol by [KtSymbol.createPointer]
 *
 * To create analysis session consider using [analyze]
 */
abstract class KtAnalysisSession(override val token: ValidityToken) : ValidityTokenOwner {
    protected abstract val smartCastProvider: KtSmartCastProvider
    protected abstract val typeProvider: KtTypeProvider
    protected abstract val diagnosticProvider: KtDiagnosticProvider
    protected abstract val scopeProvider: KtScopeProvider
    protected abstract val containingDeclarationProvider: KtSymbolContainingDeclarationProvider
    protected abstract val symbolProvider: KtSymbolProvider
    protected abstract val callResolver: KtCallResolver


    /// TODO: get rid of
    @Deprecated("Used only in completion now, temporary")
    abstract fun createContextDependentCopy(): KtAnalysisSession


    fun KtExpression.getSmartCasts(): Collection<KtType> = smartCastProvider.getSmartCastedToTypes(this)

    fun KtExpression.getImplicitReceiverSmartCasts(): Collection<ImplicitReceiverSmartCast> = smartCastProvider.getImplicitReceiverSmartCasts(this)

    fun KtExpression.getKtType(): KtType = typeProvider.getKtExpressionType(this)

    fun KtDeclaration.getReturnKtType(): KtType = typeProvider.getReturnTypeForKtDeclaration(this)

    fun KtElement.getDiagnostics(): Collection<Diagnostic> = diagnosticProvider.getDiagnosticsForElement(this)

    fun KtSymbolWithKind.getContainingSymbol(): KtSymbolWithKind? = containingDeclarationProvider.getContainingDeclaration(this)

    fun KtClassOrObjectSymbol.getMemberScope(): KtMemberScope = scopeProvider.getMemberScope(this)

    fun KtClassOrObjectSymbol.getDeclaredMemberScope(): KtDeclaredMemberScope = scopeProvider.getDeclaredMemberScope(this)

    fun KtPackageSymbol.getPackageScope(): KtPackageScope = scopeProvider.getPackageScope(this)

    fun List<KtScope>.asCompositeScope(): KtCompositeScope = scopeProvider.getCompositeScope(this)

    fun KtType.getTypeScope(): KtScope? = scopeProvider.getTypeScope(this)

    fun KtFile.getScopeContextForPosition(originalPosition: PsiElement, positionInFakeFile: KtElement): KtScopeContext =
        scopeProvider.getScopeContextForPosition(this, originalPosition, positionInFakeFile)

    fun KtDeclaration.getSymbol(): KtSymbol = symbolProvider.getSymbol(this)

    fun KtParameter.getParameterSymbol(): KtParameterSymbol = symbolProvider.getParameterSymbol(this)

    fun KtNamedFunction.getFunctionSymbol(): KtFunctionSymbol = symbolProvider.getFunctionSymbol(this)

    fun KtConstructor<*>.getConstructorSymbol(): KtConstructorSymbol = symbolProvider.getConstructorSymbol(this)

    fun KtTypeParameter.getTypeParameterSymbol(): KtTypeParameterSymbol = symbolProvider.getTypeParameterSymbol(this)

    fun KtTypeAlias.getTypeAliasSymbol(): KtTypeAliasSymbol = symbolProvider.getTypeAliasSymbol(this)

    fun KtEnumEntry.getEnumEntrySymbol(): KtEnumEntrySymbol = symbolProvider.getEnumEntrySymbol(this)

    fun KtNamedFunction.getAnonymousFunctionSymbol(): KtAnonymousFunctionSymbol = symbolProvider.getAnonymousFunctionSymbol(this)

    fun KtLambdaExpression.getAnonymousFunctionSymbol(): KtAnonymousFunctionSymbol = symbolProvider.getAnonymousFunctionSymbol(this)

    fun KtProperty.getVariableSymbol(): KtVariableSymbol = symbolProvider.getVariableSymbol(this)

    fun KtClassOrObject.getClassOrObjectSymbol(): KtClassOrObjectSymbol = symbolProvider.getClassOrObjectSymbol(this)

    fun KtPropertyAccessor.getPropertyAccessorSymbol(): KtPropertyAccessorSymbol = symbolProvider.getPropertyAccessorSymbol(this)

    /**
     * @return symbol with specified [this@getClassOrObjectSymbolByClassId] or `null` in case such symbol is not found
     */
    fun ClassId.getCorrespondingToplevelClassOrObjectSymbol(): KtClassOrObjectSymbol? = symbolProvider.getClassOrObjectSymbolByClassId(this)

    fun FqName.getContainingCallableSymbolsWithName(name: Name): Sequence<KtSymbol> = symbolProvider.getTopLevelCallableSymbols(this, name)

    fun <S : KtSymbol> KtSymbolPointer<S>.restoreSymbol(): S? = restoreSymbol(this@KtAnalysisSession)

    fun KtCallExpression.resolveCall(): CallInfo? = callResolver.resolveCall(this)

    fun KtBinaryExpression.resolveCall(): CallInfo? = callResolver.resolveCall(this)

    fun KtReference.resolveToSymbols(): Collection<KtSymbol> {
        check(this is KtSymbolBasedReference) { "To get reference symbol the one should be KtSymbolBasedReference" }
        return this@KtAnalysisSession.resolveToSymbols()
    }

    fun KtSimpleReference<*>.resolveToSymbol(): KtSymbol? {
        check(this is KtSymbolBasedReference) { "To get reference symbol the one should be KtSymbolBasedReference but was ${this::class}" }
        return resolveToSymbols().singleOrNull()
    }
}
