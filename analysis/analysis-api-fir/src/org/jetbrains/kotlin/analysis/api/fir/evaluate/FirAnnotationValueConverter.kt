/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.evaluate

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.*
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.getTargetType
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedTypeQualifierError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.ArrayFqNames

internal object FirAnnotationValueConverter {
    fun toNamedConstantValue(
        analysisSession: KaSession,
        argumentMapping: Map<Name, FirExpression>,
        builder: KaSymbolByFirBuilder,
    ): List<KaNamedAnnotationValue> = argumentMapping.map { (name, expression) ->
        KaBaseNamedAnnotationValue(
            name,
            expression.convertConstantExpression(builder) ?: KaUnsupportedAnnotationValueImpl(analysisSession.token),
        )
    }

    private fun FirLiteralExpression.convertConstantExpression(
        analysisSession: KaSession
    ): KaAnnotationValue.ConstantValue? {
        val expression = psi as? KtElement

        @OptIn(UnresolvedExpressionTypeAccess::class)
        val type = coneTypeOrNull
        val constantValue = when {
            value == null -> KaNullConstantValueImpl(expression)
            type == null -> KaConstantValueFactory.createConstantValue(value, psi as? KtElement)
            type.isBoolean -> KaBooleanConstantValueImpl(value as Boolean, expression)
            type.isChar -> KaCharConstantValueImpl((value as? Char) ?: (value as Number).toInt().toChar(), expression)
            type.isByte -> KaByteConstantValueImpl((value as Number).toByte(), expression)
            type.isUByte -> KaUnsignedByteConstantValueImpl((value as Number).toByte().toUByte(), expression)
            type.isShort -> KaShortConstantValueImpl((value as Number).toShort(), expression)
            type.isUShort -> KaUnsignedShortConstantValueImpl((value as Number).toShort().toUShort(), expression)
            type.isInt -> KaIntConstantValueImpl((value as Number).toInt(), expression)
            type.isUInt -> KaUnsignedIntConstantValueImpl((value as Number).toInt().toUInt(), expression)
            type.isLong -> KaLongConstantValueImpl((value as Number).toLong(), expression)
            type.isULong -> KaUnsignedLongConstantValueImpl((value as Number).toLong().toULong(), expression)
            type.isString -> KaStringConstantValueImpl(value.toString(), expression)
            type.isFloat -> KaFloatConstantValueImpl((value as Number).toFloat(), expression)
            type.isDouble -> KaDoubleConstantValueImpl((value as Number).toDouble(), expression)
            else -> null
        }

        return constantValue?.let { KaConstantAnnotationValueImpl(it, analysisSession.token) }
    }

    private fun Collection<FirExpression>.convertVarargsExpression(
        builder: KaSymbolByFirBuilder,
    ): Pair<Collection<KaAnnotationValue>, KtElement?> {
        var representativePsi: KtElement? = null
        val flattenedVarargs = buildList {
            for (expr in this@convertVarargsExpression) {
                val converted = expr.convertConstantExpression(builder) ?: continue

                if ((expr is FirSpreadArgumentExpression || expr is FirNamedArgumentExpression) && converted is KaAnnotationValue.ArrayValue) {
                    addAll(converted.values)
                } else {
                    add(converted)
                }
                representativePsi = representativePsi ?: converted.sourcePsi
            }
        }

        return flattenedVarargs to representativePsi
    }


    fun toConstantValue(
        firExpression: FirExpression,
        builder: KaSymbolByFirBuilder,
    ): KaAnnotationValue? = firExpression.convertConstantExpression(builder)

    private fun FirExpression.convertConstantExpression(builder: KaSymbolByFirBuilder): KaAnnotationValue? {
        val session = builder.rootSession
        val token = builder.analysisSession.token
        val sourcePsi = psi as? KtElement

        return when (this) {
            is FirLiteralExpression -> convertConstantExpression(builder.analysisSession)
            is FirNamedArgumentExpression -> {
                expression.convertConstantExpression(builder)
            }

            is FirSpreadArgumentExpression -> {
                expression.convertConstantExpression(builder)
            }

            is FirVarargArgumentsExpression -> {
                // Vararg arguments may have multiple independent expressions associated.
                // Choose one to be the representative PSI value for the entire assembled argument.
                val (annotationValues, representativePsi) = arguments.convertVarargsExpression(builder)
                KaArrayAnnotationValueImpl(annotationValues, representativePsi ?: sourcePsi, token)
            }

            is FirArrayLiteral -> {
                // Desugared collection literals.
                KaArrayAnnotationValueImpl(argumentList.arguments.convertVarargsExpression(builder).first, sourcePsi, token)
            }

            is FirFunctionCall -> {
                val reference = calleeReference as? FirResolvedNamedReference ?: return null
                when (val resolvedSymbol = reference.resolvedSymbol) {
                    is FirConstructorSymbol -> {
                        val argumentMapping = buildMap {
                            for ((argumentExpression, valueParameter) in resolvedArgumentMapping?.entries.orEmpty()) {
                                put(valueParameter.name, argumentExpression)
                            }
                        }

                        createNestedAnnotation(builder, psi, resolvedSymbol, argumentMapping)
                    }

                    is FirNamedFunctionSymbol -> {
                        // arrayOf call with a single vararg argument.
                        if (resolvedSymbol.callableId.asSingleFqName() in ArrayFqNames.ARRAY_CALL_FQ_NAMES)
                            argumentList.arguments.singleOrNull()?.convertConstantExpression(builder)
                                ?: KaArrayAnnotationValueImpl(emptyList(), sourcePsi, token)
                        else null
                    }

                    is FirEnumEntrySymbol -> {
                        KaEnumEntryAnnotationValueImpl(resolvedSymbol.callableId, sourcePsi, token)
                    }

                    else -> null
                }
            }

            is FirAnnotation -> {
                val annotationSymbol = annotationTypeRef.toRegularClassSymbol(session) ?: return null
                val constructorSymbol = annotationSymbol.primaryConstructorIfAny(session) ?: return null
                createNestedAnnotation(builder, psi, constructorSymbol, argumentMapping.mapping)
            }

            is FirPropertyAccessExpression -> {
                val reference = calleeReference as? FirResolvedNamedReference ?: return null
                when (val resolvedSymbol = reference.resolvedSymbol) {
                    is FirEnumEntrySymbol -> {
                        KaEnumEntryAnnotationValueImpl(resolvedSymbol.callableId, sourcePsi, token)
                    }

                    else -> null
                }
            }

            is FirEnumEntryDeserializedAccessExpression -> {
                KaEnumEntryAnnotationValueImpl(CallableId(enumClassId, enumEntryName), sourcePsi, token)
            }

            is FirGetClassCall -> {
                val coneType = getTargetType()?.fullyExpandedType(session)?.lowerBoundIfFlexible()

                if (coneType is ConeClassLikeType && coneType !is ConeErrorType) {
                    val classId = coneType.lookupTag.classId
                    val type = builder.typeBuilder.buildKtType(coneType)
                    KaClassLiteralAnnotationValueImpl(type, classId, sourcePsi, token)
                } else {
                    val classId = computeErrorCallClassId(this)
                    val diagnostic = classId?.let(::ConeUnresolvedSymbolError) ?: ConeSimpleDiagnostic("Unresolved class reference")
                    val errorType = builder.typeBuilder.buildKtType(ConeErrorType(diagnostic))
                    KaClassLiteralAnnotationValueImpl(errorType, classId, sourcePsi, token)
                }
            }

            else -> null
        } ?: FirCompileTimeConstantEvaluator.evaluate(this)
            ?.convertConstantExpression(builder.analysisSession)
    }

    private fun createNestedAnnotation(
        builder: KaSymbolByFirBuilder,
        psi: PsiElement?,
        resolvedSymbol: FirConstructorSymbol,
        argumentMapping: Map<Name, FirExpression>
    ): KaAnnotationValue? {
        val classSymbol = resolvedSymbol.getContainingClassSymbol() ?: return null
        if (classSymbol.classKind != ClassKind.ANNOTATION_CLASS) {
            return null
        }

        val token = builder.analysisSession.token

        return KaNestedAnnotationAnnotationValueImpl(
            KaAnnotationImpl(
                classId = resolvedSymbol.callableId.classId,
                psi = psi as? KtCallElement,
                useSiteTarget = null,
                lazyArguments = if (argumentMapping.isNotEmpty())
                    lazy { toNamedConstantValue(builder.analysisSession, argumentMapping, builder) }
                else
                    lazyOf(emptyList()),
                constructorSymbol = with(builder.analysisSession) {
                    builder.functionBuilder.buildConstructorSymbol(resolvedSymbol)
                },
                token = token
            ),
            token
        )
    }

    private fun computeErrorCallClassId(call: FirGetClassCall): ClassId? {
        val qualifierParts = mutableListOf<String?>()

        fun process(expression: FirExpression) {
            val errorType = expression.resolvedType as? ConeErrorType
            val unresolvedName = when (val diagnostic = errorType?.diagnostic) {
                is ConeUnresolvedTypeQualifierError -> diagnostic.qualifier
                is ConeUnresolvedNameError -> diagnostic.qualifier
                else -> null
            }
            qualifierParts += unresolvedName
            if (errorType != null && expression is FirPropertyAccessExpression) {
                expression.explicitReceiver?.let { process(it) }
            }
        }

        process(call.argument)

        val fqNameString = qualifierParts.asReversed().filterNotNull().takeIf { it.isNotEmpty() }?.joinToString(".")
        if (fqNameString != null) {
            val fqNameUnsafe = FqNameUnsafe(fqNameString)
            if (fqNameUnsafe.isSafe) {
                return ClassId.topLevel(fqNameUnsafe.toSafe())
            }
        }

        return null
    }
}
