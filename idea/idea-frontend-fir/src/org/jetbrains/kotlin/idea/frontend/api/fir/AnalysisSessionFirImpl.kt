/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.transformers.firClassLike
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.idea.fir.*
import org.jetbrains.kotlin.idea.fir.low.level.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.idea.references.FirReferenceResolveHelper
import org.jetbrains.kotlin.idea.references.FirReferenceResolveHelper.toTargetPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

class AnalysisSessionFirImpl : FrontendAnalysisSession() {

    override fun getSmartCastedToTypes(expression: KtExpression): Collection<KotlinTypeMarker>? {
        // TODO filter out not used smartcasts
        return (expression.toFir() as? FirExpressionWithSmartcast)?.typesFromSmartCast
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getImplicitReceiverSmartCasts(expression: KtExpression): Collection<ImplicitReceiverSmartCast> {
        // TODO filter out not used smartcasts
        val qualifiedExpression = expression.getOrBuildFirSafe<FirQualifiedAccessExpression>() ?: return emptyList()
        if (qualifiedExpression.dispatchReceiver !is FirExpressionWithSmartcast
            && qualifiedExpression.extensionReceiver !is FirExpressionWithSmartcast
        ) return emptyList()
        return buildList {
            (qualifiedExpression.dispatchReceiver as? FirExpressionWithSmartcast)?.let { smartCasted ->
                ImplicitReceiverSmartCast(smartCasted.typesFromSmartCast, ImplicitReceiverSmartcastKind.DISPATCH)
            }?.let(::add)
            (qualifiedExpression.extensionReceiver as? FirExpressionWithSmartcast)?.let { smartCasted ->
                ImplicitReceiverSmartCast(smartCasted.typesFromSmartCast, ImplicitReceiverSmartcastKind.EXTENSION)
            }?.let(::add)
        }
    }

    override fun renderType(type: KotlinTypeMarker): String =
        type.asConeType().render()

    override fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KotlinTypeMarker? =
        declaration.toFir<FirCallableDeclaration<*>>()?.returnTypeRef?.coneTypeSafe()

    override fun getKtExpressionType(expression: KtExpression): ConeKotlinType? =
        expression.toFir<FirExpression>()?.typeRef?.coneTypeSafe()

    override fun isSubclassOf(klass: KtClassOrObject, superClassId: ClassId): Boolean {
        var result = false
        forEachSubClass(klass.toFir() ?: return false) { type ->
            result = result || type.firClassLike(klass.session)?.symbol?.classId == superClassId
        }
        return result
    }

    override fun getDiagnosticsForElement(element: KtElement): Collection<Diagnostic> =
        LowLevelFirApiFacade.getDiagnosticsFor(element)

    override fun resolveCall(call: KtBinaryExpression): CallInfo? {
        val firCall = call.toFir<FirFunctionCall>() ?: return null
        return resolveCall(firCall, call)
    }

    override fun resolveCall(call: KtCallExpression): CallInfo? {
        val firCall = call.toFir<FirFunctionCall>() ?: return null
        return resolveCall(firCall, call)
    }

    private fun resolveCall(firCall: FirFunctionCall, callExpression: KtExpression): CallInfo? {
        val session = callExpression.session
        val resolvedFunctionPsi = firCall.calleeReference.toTargetPsi(session)
        val resolvedCalleeSymbol = (firCall.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
        return when {
            resolvedCalleeSymbol is FirConstructorSymbol -> {
                ConstructorCallInfo//todo use proper constructor info
            }
            firCall.dispatchReceiver is FirQualifiedAccessExpression && firCall.isImplicitFunctionCall() -> {
                val target = with(FirReferenceResolveHelper) {
                    val calleeReference = (firCall.dispatchReceiver as FirQualifiedAccessExpression).calleeReference
                    calleeReference.toTargetPsi(session)
                }
                when (target) {
                    null -> null
                    is KtValVarKeywordOwner, is PsiField -> {
                        val functionSymbol =
                            (firCall.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol
                        when (functionSymbol?.callableId) {
                            null -> null
                            in kotlinFunctionInvokeCallableIds -> VariableAsFunctionCallInfo(target, functionSymbol.fir.isSuspend)
                            else -> (resolvedFunctionPsi as? KtNamedFunction)?.let { VariableAsFunctionLikeCallInfo(target, it) }
                        }
                    }
                    else -> resolvedFunctionPsi?.asSimpleFunctionCall()
                }
            }
            else -> resolvedFunctionPsi?.asSimpleFunctionCall()
        }
    }

    private fun PsiElement.asSimpleFunctionCall() = when (this) {
        is KtNamedFunction -> SimpleKtFunctionCallInfo(this)
        is PsiMethod -> SimpleJavaFunctionCallInfo(this)
        else -> null
    }

    private inline fun <reified F : FirElement> KtElement.toFir(phase: FirResolvePhase = FirResolvePhase.BODY_RESOLVE): F? =
        getOrBuildFir(phase) as? F

    private fun forEachSubClass(firClass: FirClass<*>, action: (FirResolvedTypeRef) -> Unit) {
        firClass.superTypeRefs.forEach { superType ->
            (superType as? FirResolvedTypeRef)?.let(action)
            (superType.firClassLike(firClass.session) as? FirClass<*>?)?.let { forEachSubClass(it, action) }
        }
    }

    companion object {
        private fun KotlinTypeMarker.asConeType(): ConeKotlinType =
            this as? ConeKotlinType ?: error("$this should be ConeKotlinType")

        private val kotlinFunctionInvokeCallableIds = (0..23).flatMapTo(hashSetOf()) { arity ->
            listOf(
                CallableId(KotlinBuiltIns.getFunctionClassId(arity), Name.identifier("invoke")),
                CallableId(KotlinBuiltIns.getSuspendFunctionClassId(arity), Name.identifier("invoke"))
            )
        }
    }
}