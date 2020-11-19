/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory0
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperatorCall
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.util.OperatorNameConventions.BINARY_OPERATION_NAMES
import org.jetbrains.kotlin.util.OperatorNameConventions.PLUS
import org.jetbrains.kotlin.util.OperatorNameConventions.TO_STRING
import org.jetbrains.kotlin.util.OperatorNameConventions.UNARY_OPERATION_NAMES

object FirAnnotationArgumentChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirAnnotationContainer) return
        for (declarationOfAnnotation in declaration.annotations) {
            for ((arg, _) in declarationOfAnnotation.argumentMapping ?: continue) {
                val expression = (arg as? FirNamedArgumentExpression)?.expression ?: arg

                checkAnnotationArgumentWithSubElements(expression, context.session, reporter)
                    ?.let { reporter.report(getFirSourceElement(expression), it) }
            }
        }
    }

    private fun checkAnnotationArgumentWithSubElements(
        expression: FirExpression,
        session: FirSession,
        reporter: DiagnosticReporter
    ): FirDiagnosticFactory0<FirSourceElement, KtExpression>? {
        when (expression) {
            is FirArrayOfCall -> {
                var usedNonConst = false

                for (arg in expression.argumentList.arguments) {
                    val sourceForReport = getFirSourceElement(arg)

                    when (val err = checkAnnotationArgumentWithSubElements(arg, session, reporter)) {
                        null -> {
                            //DO NOTHING
                        }
                        else -> {
                            if (err != FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL) usedNonConst = true
                            reporter.report(sourceForReport, err)
                        }
                    }
                }

                if (usedNonConst) return FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION
            }
            is FirVarargArgumentsExpression -> {
                for (arg in expression.arguments)
                    checkAnnotationArgumentWithSubElements(arg, session, reporter)
                        ?.let { reporter.report(getFirSourceElement(arg), it) }
            }
            else ->
                return checkAnnotationArgument(expression, session)
        }
        return null
    }

    private fun checkAnnotationArgument(
        expression: FirExpression,
        session: FirSession,
    ): FirDiagnosticFactory0<FirSourceElement, KtExpression>? {
        val expressionSymbol = expression.toResolvedCallableSymbol()
            ?.fir
        val classKindOfParent = (expressionSymbol
            ?.getReferencedClass(session) as? FirRegularClass)
            ?.classKind

        when {
            expression is FirConstExpression<*>
                    || expressionSymbol is FirEnumEntry
                    || (expressionSymbol as? FirMemberDeclaration)?.isConst == true
                    || expressionSymbol is FirConstructor && classKindOfParent == ClassKind.ANNOTATION_CLASS -> {
                //DO NOTHING
            }
            classKindOfParent == ClassKind.ENUM_CLASS -> {
                return FirErrors.ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST
            }
            expression is FirComparisonExpression -> {
                return checkAnnotationArgument(expression.compareToCall, session)
            }
            expression is FirIntegerOperatorCall -> {
                for (exp in (expression as FirCall).arguments.plus(expression.dispatchReceiver))
                    checkAnnotationArgument(exp, session).let { return it }
            }
            expression is FirStringConcatenationCall || expression is FirEqualityOperatorCall -> {
                for (exp in (expression as FirCall).arguments)
                    checkAnnotationArgument(exp, session).let { return it }
            }
            (expression is FirGetClassCall) -> {
                var coneType = (expression as? FirCall)
                    ?.argument
                    ?.typeRef
                    ?.coneType

                if (coneType is ConeClassErrorType)
                    return FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST

                while (coneType?.classId == StandardClassIds.Array)
                    coneType = (coneType.lowerBoundIfFlexible().typeArguments.first() as? ConeKotlinTypeProjection)?.type ?: break

                return when {
                    coneType is ConeTypeParameterType ->
                        FirErrors.ANNOTATION_ARGUMENT_KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
                    (expression as FirCall).argument !is FirResolvedQualifier ->
                        FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL
                    else ->
                        null
                }
            }
            expressionSymbol == null -> {
                //DO NOTHING
            }
            expressionSymbol is FirField -> {
                //TODO: fix checking of Java fields initializer
                if (
                    !(expressionSymbol as FirMemberDeclaration).status.isStatic
                    || (expressionSymbol as FirMemberDeclaration).status.modality != Modality.FINAL
                )
                    return FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
            }
            expression is FirFunctionCall -> {
                val calleeReference = expression.calleeReference
                if (calleeReference is FirErrorNamedReference) {
                    return null
                }
                if (expression.typeRef.coneType.classId == StandardClassIds.KClass) {
                    return FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL
                }

                //TODO: UNRESOLVED REFERENCE
                if (expression.dispatchReceiver is FirThisReceiverExpression) {
                    return null
                }

                when (calleeReference.name) {
                    in BINARY_OPERATION_NAMES, in UNARY_OPERATION_NAMES -> {
                        val receiverClassId = expression.dispatchReceiver.typeRef.coneType.classId

                        for (exp in (expression as FirCall).arguments.plus(expression.dispatchReceiver)) {
                            val expClassId = exp.typeRef.coneType.classId

                            if (calleeReference.name == PLUS
                                && expClassId != receiverClassId
                                && (expClassId !in StandardClassIds.primitiveTypes || receiverClassId !in StandardClassIds.primitiveTypes)
                            )
                                return FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST

                            checkAnnotationArgument(exp, session)?.let { return it }
                        }
                    }
                    else -> {
                        if (expression.arguments.isNotEmpty() || calleeReference !is FirResolvedNamedReference) {
                            return FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
                        }
                        val symbol = calleeReference.resolvedSymbol as? FirCallableSymbol
                        if (calleeReference.name == TO_STRING ||
                            calleeReference.name in CONVERSION_NAMES && symbol?.callableId?.packageName?.asString() == "kotlin"
                        ) {
                            return checkAnnotationArgument(expression.dispatchReceiver, session)
                        }
                        return FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
                    }
                }
            }
            expression is FirQualifiedAccessExpression -> {

                when {
                    (expressionSymbol as FirProperty).isLocal || expressionSymbol.symbol.callableId.className?.isRoot == false ->
                        return FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
                    expression.typeRef.coneType.classId == StandardClassIds.KClass ->
                        return FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL

                    //TODO: UNRESOLVED REFERENCE
                    expression.dispatchReceiver is FirThisReceiverExpression ->
                        return null
                }

                return when ((expressionSymbol as? FirProperty)?.initializer) {
                    is FirConstExpression<*> -> {
                        if ((expressionSymbol as? FirVariable)?.isVal == true)
                            FirErrors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION
                        else
                            FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
                    }
                    is FirGetClassCall ->
                        FirErrors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL
                    else ->
                        FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
                }
            }
            else ->
                return FirErrors.ANNOTATION_ARGUMENT_MUST_BE_CONST
        }
        return null
    }

    private fun FirTypedDeclaration?.getReferencedClass(session: FirSession): FirSymbolOwner<*>? =
        this?.returnTypeRef
            ?.coneTypeSafe<ConeLookupTagBasedType>()
            ?.lookupTag
            ?.toSymbol(session)
            ?.fir

    private fun getFirSourceElement(expression: FirExpression): FirSourceElement? =
        when {
            expression is FirFunctionCall && expression.calleeReference.name == TO_STRING ->
                getParentOfFirSourceElement(getParentOfFirSourceElement(expression.source))
            expression is FirFunctionCall ->
                expression.source
            (expression as? FirQualifiedAccess)?.explicitReceiver != null ->
                getParentOfFirSourceElement(expression.source)
            else ->
                expression.source
        }

    private fun getParentOfFirSourceElement(source: FirSourceElement?): FirSourceElement? =
        when (source) {
            is FirPsiSourceElement<*> ->
                source.psi.parent.toFirPsiSourceElement()
            is FirLightSourceElement -> {
                val elementOfParent = source.treeStructure.getParent(source.lighterASTNode) ?: source.lighterASTNode

                elementOfParent.toFirLightSourceElement(source.treeStructure)
            }
            else ->
                source
        }

    private inline fun <reified T : FirSourceElement, P : PsiElement> DiagnosticReporter.report(
        source: T?,
        factory: FirDiagnosticFactory0<T, P>
    ) {
        source?.let { report(factory.on(it)) }
    }

    private val CONVERSION_NAMES = listOf(
        "toInt", "toLong", "toShort", "toByte", "toFloat", "toDouble", "toChar", "toBoolean"
    ).mapTo(hashSetOf()) { Name.identifier(it) }
}