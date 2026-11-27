/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.evaluate

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.fir.*
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.*
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.fir.declarations.getTargetType
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedSymbolError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedTypeQualifierError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.resolve.ArrayFqNames

internal object FirAnnotationValueConverter {
    context(analysisSession: KaFirSession)
    fun toNamedConstantValue(
        argumentMapping: Map<Name, FirExpression>,
    ): List<KaNamedAnnotationValue> = argumentMapping.map { [name, expression] ->
        KaFirNamedAnnotationValue(
            name,
            expression.convertConstantExpression() ?: KaUnsupportedAnnotationValueImpl(analysisSession.token),
        )
    }

    context(session: KaSession)
    private fun FirLiteralExpression.convertConstantExpression(): KaAnnotationValue.ConstantValue? {
        val expression = psi as? KtElement

        @OptIn(UnresolvedExpressionTypeAccess::class)
        val type = coneTypeOrNull
        val constantValue = when {
            value == null -> KaNullConstantValueImpl(expression)
            type == null -> KaConstantValueFactory.createConstantValue(value, psi as? KtElement)
            type.isBoolean -> KaBooleanConstantValueImpl(value as Boolean, expression)
            type.isChar -> KaCharConstantValueImpl((value as? Char) ?: (value as Number).toInt().toChar(), expression)
            type.isByte -> KaByteConstantValueImpl((value as Number).toByte(), expression)
            type.isUByte -> KaUnsignedByteConstantValueImpl((value as? UByte) ?: (value as Number).toByte().toUByte(), expression)
            type.isShort -> KaShortConstantValueImpl((value as Number).toShort(), expression)
            type.isUShort -> KaUnsignedShortConstantValueImpl((value as? UShort) ?: (value as Number).toShort().toUShort(), expression)
            type.isInt -> KaIntConstantValueImpl((value as Number).toInt(), expression)
            type.isUInt -> KaUnsignedIntConstantValueImpl((value as? UInt) ?: (value as Number).toInt().toUInt(), expression)
            type.isLong -> KaLongConstantValueImpl((value as Number).toLong(), expression)
            type.isULong -> KaUnsignedLongConstantValueImpl((value as? ULong) ?: (value as Number).toLong().toULong(), expression)
            type.isString -> KaStringConstantValueImpl(value.toString(), expression)
            type.isFloat -> KaFloatConstantValueImpl((value as Number).toFloat(), expression)
            type.isDouble -> KaDoubleConstantValueImpl((value as Number).toDouble(), expression)
            else -> null
        }

        return constantValue?.let { KaConstantAnnotationValueImpl(it, session.token) }
    }

    context(analysisSession: KaFirSession)
    private fun Collection<FirExpression>.convertVarargsExpression(
    ): Collection<KaAnnotationValue> {
        val flattenedVarargs = buildList {
            for (expr in this@convertVarargsExpression) {
                val converted = expr.convertConstantExpression() ?: continue
                add(converted)
            }
        }

        return flattenedVarargs
    }


    context(analysisSession: KaFirSession)
    fun toConstantValue(
        firExpression: FirExpression,
    ): KaAnnotationValue? = firExpression.convertConstantExpression()

    context(analysisSession: KaFirSession)
    private fun FirExpression.convertConstantExpression(): KaAnnotationValue? {
        val session = analysisSession.firSession
        val token = analysisSession.token
        val sourcePsi = psi as? KtElement

        return when (this) {
            is FirLiteralExpression -> convertConstantExpression()
            is FirNamedArgumentExpression -> {
                expression.convertConstantExpression()
            }

            is FirSpreadArgumentExpression -> {
                expression.convertConstantExpression()
            }

            is FirVarargArgumentsExpression -> {
                // Vararg arguments set their source to the first component.
                // This component may also be spread / named.
                // In this case, unwrap it.
                val representativePsi = (sourcePsi as? KtValueArgument)?.getArgumentExpression() ?: sourcePsi

                val annotationValues = arguments.convertVarargsExpression()
                KaArrayAnnotationValueImpl(annotationValues, representativePsi, token)
            }

            is FirCollectionLiteral -> {
                // Desugared collection literals.
                KaArrayAnnotationValueImpl(argumentList.arguments.convertVarargsExpression(), sourcePsi, token)
            }

            is FirFunctionCall -> {
                val reference = calleeReference as? FirResolvedNamedReference ?: return null
                when (val resolvedSymbol = reference.resolvedSymbol) {
                    is FirConstructorSymbol -> {
                        val annotationSymbol = resolvedType.toRegularClassSymbol(session) ?: return null
                        val argumentMapping = buildMap {
                            for ([argumentExpression, valueParameter] in resolvedArgumentMapping?.entries.orEmpty()) {
                                put(valueParameter.name, argumentExpression)
                            }
                        }

                        createNestedAnnotation(psi, annotationSymbol, argumentMapping)
                    }

                    is FirNamedFunctionSymbol -> {
                        // arrayOf call with a single vararg argument.
                        if (resolvedSymbol.callableId.asSingleFqName() in ArrayFqNames.ARRAY_CALL_FQ_NAMES)
                            argumentList.arguments.singleOrNull()?.convertConstantExpression()
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
                createNestedAnnotation(psi, annotationSymbol, argumentMapping.mapping)
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
                    val type = firSymbolBuilder.typeBuilder.buildKtType(coneType)
                    KaClassLiteralAnnotationValueImpl(type, classId, sourcePsi, token)
                } else {
                    val classId = computeErrorCallClassId(this)
                    val diagnostic = classId?.let(::ConeUnresolvedSymbolError) ?: ConeSimpleDiagnostic("Unresolved class reference")
                    val errorType = firSymbolBuilder.typeBuilder.buildKtType(ConeErrorType(diagnostic))
                    KaClassLiteralAnnotationValueImpl(errorType, classId, sourcePsi, token)
                }
            }

            else -> null
        } ?: FirCompileTimeConstantEvaluator.evaluate(this, analysisSession.firSession)
            ?.convertConstantExpression()
    }

    context(analysisSession: KaFirSession)
    private fun createNestedAnnotation(
        psi: PsiElement?,
        annotationSymbol: FirRegularClassSymbol,
        argumentMapping: Map<Name, FirExpression>
    ): KaAnnotationValue? {
        val primaryConstructor = annotationSymbol.findAnnotationConstructor(session = analysisSession.firSession) ?: return null
        val token = analysisSession.token
        return KaNestedAnnotationAnnotationValueImpl(
            KaAnnotationImpl(
                classId = annotationSymbol.classId,
                psi = psi.asKtCallElement(),
                lazyArguments = if (argumentMapping.isNotEmpty())
                    lazy { toNamedConstantValue(argumentMapping) }
                else
                    lazyOf(emptyList()),
                constructorSymbol = firSymbolBuilder.functionBuilder.buildConstructorSymbol(primaryConstructor),
                token = token
            ),
            token,
        )
    }

    private fun PsiElement?.asKtCallElement(): KtCallElement? =
        this as? KtCallElement ?: (this as? KtDotQualifiedExpression)?.selectorExpression as? KtCallElement

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
