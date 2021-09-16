/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.api.fir.utils

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.utils.primaryConstructor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.withFirDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ArrayFqNames

internal fun PsiElement.unwrap(): PsiElement {
    return when (this) {
        is KtExpression -> this.unwrap()
        else -> this
    }
}

internal fun KtExpression.unwrap(): KtExpression {
    return when (this) {
        is KtLabeledExpression -> baseExpression?.unwrap()
        is KtAnnotatedExpression -> baseExpression?.unwrap()
        is KtObjectLiteralExpression -> objectDeclaration
        is KtFunctionLiteral -> (parent as? KtLambdaExpression)?.unwrap()
        else -> this
    } ?: this
}

internal fun FirNamedReference.getReferencedElementType(resolveState: FirModuleResolveState): ConeKotlinType {
    val symbols = when (this) {
        is FirResolvedNamedReference -> listOf(resolvedSymbol)
        is FirErrorNamedReference -> getCandidateSymbols()
        else -> error("Unexpected ${this::class}")
    }
    val firCallableDeclaration = symbols.singleOrNull()?.fir as? FirCallableDeclaration
        ?: return ConeClassErrorType(ConeUnresolvedNameError(name))

    return firCallableDeclaration.withFirDeclaration(ResolveType.CallableReturnType, resolveState) {
        it.returnTypeRef.coneType
    }
}

internal fun mapAnnotationParameters(annotation: FirAnnotation, session: FirSession): Map<String, FirExpression> {
    if (annotation.resolved) return annotation.argumentMapping.mapping.mapKeys { (name, _) -> name.identifier }
    if (annotation !is FirAnnotationCall) return emptyMap()
    val annotationCone = annotation.annotationTypeRef.coneType as? ConeClassLikeType ?: return emptyMap()

    val annotationPrimaryCtor = (annotationCone.lookupTag.toSymbol(session)?.fir as? FirRegularClass)?.primaryConstructor
    val annotationCtorParameterNames = annotationPrimaryCtor?.valueParameters?.map { it.name }

    val resultSet = mutableMapOf<String, FirExpression>()

    val namesSequence = annotationCtorParameterNames?.asSequence()?.iterator()

    for (argument in annotation.argumentList.arguments.filterIsInstance<FirNamedArgumentExpression>()) {
        resultSet[argument.name.asString()] = argument.expression
    }

    for (argument in annotation.argumentList.arguments) {
        if (argument is FirNamedArgumentExpression) continue

        while (namesSequence != null && namesSequence.hasNext()) {
            val name = namesSequence.next().asString()
            if (!resultSet.contains(name)) {
                resultSet[name] = argument
                break
            }
        }
    }

    return resultSet
}

internal fun Map<String, FirExpression>.toNamedConstantValue(
    session: FirSession,
    firSymbolBuilder: KtSymbolByFirBuilder
): List<KtNamedConstantValue> =
    map { (name, expression) ->
        KtNamedConstantValue(
            name,
            expression.convertConstantExpression(session, firSymbolBuilder) ?: KtUnsupportedConstantValue
        )
    }

internal fun <T> FirConstExpression<T>.convertConstantExpression(): KtSimpleConstantValue<T> = KtSimpleConstantValue(kind, value)

private fun Collection<FirExpression>.convertConstantExpression(
    session: FirSession,
    firSymbolBuilder: KtSymbolByFirBuilder
): Collection<KtConstantValue> =
    mapNotNull { it.convertConstantExpression(session, firSymbolBuilder) }

private fun Collection<KtConstantValue>.toArrayConstantValueIfNecessary(): KtConstantValue {
    return if (size == 1)
        single()
    else
        KtArrayConstantValue(this)
}

internal fun FirExpression.convertConstantExpression(
    session: FirSession,
    firSymbolBuilder: KtSymbolByFirBuilder
): KtConstantValue? {
    return when (this) {
        is FirConstExpression<*> -> convertConstantExpression()
        is FirNamedArgumentExpression -> {
            expression.convertConstantExpression(session, firSymbolBuilder)
        }
        is FirVarargArgumentsExpression -> {
            arguments.convertConstantExpression(session, firSymbolBuilder).toArrayConstantValueIfNecessary()
        }
        is FirArrayOfCall -> {
            argumentList.arguments.convertConstantExpression(session, firSymbolBuilder).toArrayConstantValueIfNecessary()
        }
        is FirFunctionCall -> {
            val reference = calleeReference as? FirResolvedNamedReference ?: return null
            when (val resolvedSymbol = reference.resolvedSymbol) {
                is FirConstructorSymbol -> {
                    val classSymbol = resolvedSymbol.getContainingClassSymbol(session) ?: return null
                    if ((classSymbol.fir as? FirClass)?.classKind == ClassKind.ANNOTATION_CLASS) {
                        val resultMap = mutableMapOf<String, FirExpression>()
                        argumentMapping?.entries?.forEach { (arg, param) ->
                            resultMap[param.name.asString()] = arg
                        }
                        KtAnnotationConstantValue(
                            resolvedSymbol.callableId.className?.asString(),
                            resultMap.toNamedConstantValue(session, firSymbolBuilder)
                        )
                    } else null
                }
                is FirNamedFunctionSymbol -> {
                    if (resolvedSymbol.callableId.asSingleFqName() in ArrayFqNames.ARRAY_CALL_FQ_NAMES)
                        argumentList.arguments.convertConstantExpression(session, firSymbolBuilder).toArrayConstantValueIfNecessary()
                    else null
                }
                else -> null
            }
        }
        is FirPropertyAccessExpression -> {
            val reference = calleeReference as? FirResolvedNamedReference ?: return null
            when (val resolvedSymbol = reference.resolvedSymbol) {
                is FirEnumEntrySymbol -> {
                    KtEnumEntryValue(resolvedSymbol.fir.buildSymbol(firSymbolBuilder) as KtEnumEntrySymbol)
                }
                else -> null
            }
        }
        else -> KtUnsupportedConstantValue
    }
}

internal fun KtTypeNullability.toConeNullability() = when (this) {
    KtTypeNullability.NULLABLE -> ConeNullability.NULLABLE
    KtTypeNullability.NON_NULLABLE -> ConeNullability.NOT_NULL
    KtTypeNullability.UNKNOWN -> ConeNullability.UNKNOWN
}

/**
 * @receiver A symbol that needs to be imported
 * @param useSiteSession A use-site fir session.
 * @return An [FqName] by which this symbol can be imported (if it is possible)
 */
internal fun FirCallableSymbol<*>.computeImportableName(useSiteSession: FirSession): FqName? {
    // if classId == null, callable is topLevel
    val containingClassId = callableId.classId
        ?: return callableId.asSingleFqName()

    if (this is FirConstructorSymbol) return containingClassId.asSingleFqName()

    val containingClass = getContainingClassSymbol(useSiteSession) ?: return null

    // Java static members, enums, and object members can be imported
    val canBeImported = containingClass.origin == FirDeclarationOrigin.Java ||
            containingClass.classKind == ClassKind.ENUM_CLASS ||
            containingClass.classKind == ClassKind.OBJECT

    return if (canBeImported) callableId.asSingleFqName() else null
}
