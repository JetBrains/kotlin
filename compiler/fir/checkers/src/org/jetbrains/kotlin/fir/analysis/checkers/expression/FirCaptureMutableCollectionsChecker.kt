/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)
package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import kotlin.reflect.full.memberProperties


object FirCaptureMutableCollectionsChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val report = IEReporter(expression.source, context, reporter, FirErrors.MUTABLE_DIAGNOSTIC)
        val variableSymbol = expression.calleeReference.toResolvedVariableSymbol() ?: return
        // If it's a var, then it was analyzed in another checker.
        if (variableSymbol.isVar) return

        val type = expression.resolvedType
        if (type is ConeErrorType) return

        if (!isMutableCollection(type, context.session)) return
        val containingLambda =
            context.containingDeclarations.filterIsInstance<FirAnonymousFunctionSymbol>().lastOrNull() ?: return

        if (isDeclaredInsideLambda(variableSymbol, containingLambda)) return

        val baseReceiverSymbol = leftmostReceiverVariableSymbol(expression)

        // Case 2: the leftmost receiver in a qualified chain is a local val declared inside the lambda
        if (baseReceiverSymbol != null) {
            val declInsideLambda = isDeclaredInsideLambda(baseReceiverSymbol, containingLambda)
            if (declInsideLambda) return
        }

        if (!expression.partOfCall()) return

        val call = containingLambda.findContainingCall() ?: return
        val calledFunctionSymbol = call.toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return
        if (calledFunctionSymbol.isInline && containingLambda.inlineStatus != InlineStatus.NoInline) return

        val contractDescription = calledFunctionSymbol.resolvedContractDescription
        val hasCallsInplace = contractDescription?.effects?.any { effectDeclaration ->
            val effect = effectDeclaration.effect
            effect is ConeCallsEffectDeclaration
        }
        if (hasCallsInplace != null && hasCallsInplace) return

        report(
            IEData(
                containingLambda = containingLambda.name.asString(),
                callName = calledFunctionSymbol.name.toString(),
                variableName = variableSymbol.name.toString(),
                leftmostReceiverName = baseReceiverSymbol?.name?.asString() ?: "no receiver",
            )
        )
    }

    private val mutableIds = setOf(
        StandardClassIds.MutableList,
        StandardClassIds.MutableSet,
        StandardClassIds.MutableMap
    )

    private fun isMutableCollection(type: ConeKotlinType, session: FirSession): Boolean {
        return checkHierarchy(type, session)
    }

    private fun isMutableType(type: ConeKotlinType): Boolean {
        if (type is ConeFlexibleType) {
            val low = isMutableType(type.lowerBoundIfFlexible())
            val up = isMutableType(type.upperBoundIfFlexible())
            return low && up
        }

        if (type.isNonPrimitiveArray) {
            val projection = type.typeArguments.firstOrNull()
            if ((projection != null) && ((projection.kind == ProjectionKind.OUT)
                        || (projection.kind == ProjectionKind.STAR))
            ) {
                return false
            }
            return true
        }

        if (type.isMutableList || type.isMutableSet || type.isMutableMap || type.isArrayType) return true

        val classLike = (type as? ConeLookupTagBasedType) as? ConeClassLikeType ?: return false
        val classId = classLike.lookupTag.classId
        if (classId in mutableIds) return true

        val packageFqName = classId.packageFqName.asString()
        if (packageFqName.startsWith("java.util")) {
            return true
        }

        return false
    }

    private fun checkHierarchy(type: ConeKotlinType, session: FirSession): Boolean {
        val symbol = type.toRegularClassSymbol(session) ?: return false
        val visited = mutableSetOf<ClassId>()
        val queue = ArrayDeque<Pair<ClassId, ConeKotlinType>>()
        queue.add(symbol.classId to type)

        while (queue.isNotEmpty()) {
            val (id, type) = queue.removeFirst()
            if (!visited.add(id)) continue

            if (isMutableType(type)) return true

            val sym = session.symbolProvider.getClassLikeSymbolByClassId(id) as? FirClassSymbol<*> ?: continue

            for (superTypeRef in sym.resolvedSuperTypeRefs) {
                val superType = superTypeRef.coneType
                if (superType is ConeClassLikeType) {
                    queue.add(superType.lookupTag.classId to superType)
                }
            }
        }
        return false
    }

    context(context: CheckerContext)
    private fun FirExpression.partOfCall(): Boolean {
        val callsOrAssignments = context.callsOrAssignments
        val containingQualifiedAccess = callsOrAssignments.getOrNull(callsOrAssignments.size - 2) ?: return false
        if (this ==
            (containingQualifiedAccess as? FirQualifiedAccessExpression)?.explicitReceiver?.unwrapErrorExpression()
        ) {
            return true
        }
        val call = containingQualifiedAccess as? FirCall ?: return false
        val mapping = call.resolvedArgumentMapping ?: return false
        return mapping.keys.any { arg -> arg.unwrapErrorExpression().unwrapArgument() == this }
    }

    context(context: CheckerContext)
    private fun FirAnonymousFunctionSymbol.findContainingCall(): FirFunctionCall? {
        for (call in context.callsOrAssignments) {
            val functionCall = call as? FirFunctionCall ?: continue
            val mapping = functionCall.resolvedArgumentMapping ?: continue
            for ((argument, _) in mapping) {
                val anonymous = argument.unwrapErrorExpression()
                    .unwrapArgument() as? FirAnonymousFunctionExpression
                val af = anonymous?.anonymousFunction ?: continue
                if (af.symbol === this) return functionCall
            }
        }
        return null
    }

    private fun isDeclaredInsideLambda(
        symbol: FirVariableSymbol<*>,
        lambdaSymbol: FirAnonymousFunctionSymbol,
    ): Boolean {
        val symSource = symbol.fir.source ?: return false
        val lambdaSource = lambdaSymbol.fir.source ?: return false
        return symSource.startOffset >= lambdaSource.startOffset && symSource.endOffset <= lambdaSource.endOffset
    }

    private fun leftmostReceiverVariableSymbol(expression: FirQualifiedAccessExpression): FirVariableSymbol<*>? {
        var current: FirExpression =
            expression.explicitReceiver?.unwrapErrorExpression()?.unwrapArgument() ?: return null
        while (true) {
            when (val e = current) {
                is FirQualifiedAccessExpression -> {
                    val next = e.explicitReceiver?.unwrapErrorExpression()?.unwrapArgument()
                    if (next != null) {
                        current = next
                        continue
                    }
                    return e.calleeReference.toResolvedVariableSymbol()
                }
                is FirSafeCallExpression -> {
                    current = e.receiver.unwrapErrorExpression().unwrapArgument()
                }
                is FirCheckNotNullCall -> {
                    current = e.argument.unwrapErrorExpression().unwrapArgument()
                }
                else -> return null
            }
        }
    }
}

class IEReporter(
    private val source: KtSourceElement?,
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
    private val error: KtDiagnosticFactory1<String>,
) {
    operator fun invoke(v: IEData) {
        val dataStr = buildList {
            addAll(serializeData(v))
        }.joinToString("; ")
        val str = "$borderTag $dataStr $borderTag"
        reporter.reportOn(source, error, str, context)
    }

    private val borderTag: String = "KLEKLE"

    private fun serializeData(v: IEData): List<String> = buildList {
        v::class.memberProperties.forEach { property ->
            add("${property.name}: ${property.getter.call(v)}")
        }
    }
}

data class IEData(
    val containingLambda: String? = null,
    val callName: String? = null,
    val variableName: String? = null,
    val leftmostReceiverName: String? = null,
)