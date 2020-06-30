/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
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

class FirAnalysisSession(
    project: Project
) : FrontendAnalysisSession(project) {
   constructor(element: KtElement) : this(element.project)

    init {
        assertIsValid()
    }

    override fun getSmartCastedToTypes(expression: KtExpression): Collection<TypeInfo>? {
        assertIsValid()
        // TODO filter out not used smartcasts
        return expression.getOrBuildFirSafe<FirExpressionWithSmartcast>()?.typesFromSmartCast?.map { it.asTypeInfo(expression.session) }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun getImplicitReceiverSmartCasts(expression: KtExpression): Collection<ImplicitReceiverSmartCast> {
        assertIsValid()
        // TODO filter out not used smartcasts
        val qualifiedExpression = expression.getOrBuildFirSafe<FirQualifiedAccessExpression>() ?: return emptyList()
        if (qualifiedExpression.dispatchReceiver !is FirExpressionWithSmartcast
            && qualifiedExpression.extensionReceiver !is FirExpressionWithSmartcast
        ) return emptyList()
        val session = expression.session
        return buildList {
            (qualifiedExpression.dispatchReceiver as? FirExpressionWithSmartcast)?.let { smartCasted ->
                ImplicitReceiverSmartCast(
                    smartCasted.typesFromSmartCast.map { it.asTypeInfo(session) },
                    ImplicitReceiverSmartcastKind.DISPATCH
                )
            }?.let(::add)
            (qualifiedExpression.extensionReceiver as? FirExpressionWithSmartcast)?.let { smartCasted ->
                ImplicitReceiverSmartCast(
                    smartCasted.typesFromSmartCast.map { it.asTypeInfo(session) },
                    ImplicitReceiverSmartcastKind.EXTENSION
                )
            }?.let(::add)
        }
    }


    override fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): TypeInfo {
        assertIsValid()
        val firDeclaration = declaration.getOrBuildFirOfType<FirCallableDeclaration<*>>()
        return firDeclaration.returnTypeRef.coneType.asTypeInfo(declaration.session)
    }

    override fun getKtExpressionType(expression: KtExpression): TypeInfo {
        assertIsValid()
        return expression.getOrBuildFirOfType<FirExpression>().typeRef.coneType.asTypeInfo(expression.session)
    }

    override fun isSubclassOf(klass: KtClassOrObject, superClassId: ClassId): Boolean {
        assertIsValid()
        var result = false
        forEachSuperClass(klass.getOrBuildFirSafe() ?: return false) { type ->
            result = result || type.firClassLike(klass.session)?.symbol?.classId == superClassId
        }
        return result
    }

    override fun getDiagnosticsForElement(element: KtElement): Collection<Diagnostic> {
        assertIsValid()
        return LowLevelFirApiFacade.getDiagnosticsFor(element)
    }

    override fun resolveCall(call: KtBinaryExpression): CallInfo? {
        assertIsValid()
        val firCall = call.getOrBuildFirSafe<FirFunctionCall>() ?: return null
        return resolveCall(firCall, call)
    }

    override fun resolveCall(call: KtCallExpression): CallInfo? {
        assertIsValid()
        val firCall = when (val fir = call.getOrBuildFir()) {
            is FirFunctionCall -> fir
            is FirSafeCallExpression -> fir.regularQualifiedAccess as? FirFunctionCall
            else -> null
        } ?: return null
        return resolveCall(firCall, call)
    }

    private fun resolveCall(firCall: FirFunctionCall, callExpression: KtExpression): CallInfo? {
        val session = callExpression.session
        val resolvedFunctionPsi = firCall.calleeReference.toTargetPsi(session)
        val resolvedCalleeSymbol = (firCall.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol
        return when {
            resolvedCalleeSymbol is FirConstructorSymbol -> {
                val fir = resolvedCalleeSymbol.fir
                when (resolvedFunctionPsi) {
                    is KtClass -> KtImplicitPrimaryConstructorCallInfo(resolvedFunctionPsi)
                    is PsiClass -> JavaImplicitPrimaryConstructorCallInfo(resolvedFunctionPsi)
                    is KtConstructor<*> -> KtExplicitConstructorCallInfo(resolvedFunctionPsi, fir.isPrimary)
                    is PsiMethod -> JavaExplicitConstructorCallInfo(resolvedFunctionPsi, fir.isPrimary)
                    else -> {
                        val classId = resolvedCalleeSymbol.callableId.classId
                        val firClass = classId?.let(session.firSymbolProvider::getClassLikeSymbolByFqName)
                        when (val psiClass = firClass?.fir?.findPsi(callExpression.project)) {
                            is PsiClass -> JavaImplicitPrimaryConstructorCallInfo(psiClass)
                            is KtClass -> KtImplicitPrimaryConstructorCallInfo(psiClass)
                            else -> null
                        }
                    }
                }
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

    private fun forEachSuperClass(firClass: FirClass<*>, action: (FirResolvedTypeRef) -> Unit) {
        firClass.superTypeRefs.forEach { superType ->
            (superType as? FirResolvedTypeRef)?.let(action)
            (superType.firClassLike(firClass.session) as? FirClass<*>?)?.let { forEachSuperClass(it, action) }
        }
    }

    private fun ConeKotlinType.asTypeInfo(session: FirSession) =
        ConeTypeInfo(this, createTypeCheckingContext(session), validityToken)

    private fun createTypeCheckingContext(session: FirSession) = ConeTypeCheckerContext(
        isErrorTypeEqualsToAnything = true, // TODO?
        isStubTypeEqualsToAnything = true,  // TODO?
        session = session
    )

    companion object {

        private val kotlinFunctionInvokeCallableIds = (0..23).flatMapTo(hashSetOf()) { arity ->
            listOf(
                CallableId(KotlinBuiltIns.getFunctionClassId(arity), Name.identifier("invoke")),
                CallableId(KotlinBuiltIns.getSuspendFunctionClassId(arity), Name.identifier("invoke"))
            )
        }
    }
}