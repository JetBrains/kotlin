/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.constants.evaluate

import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.text.LiteralFormatUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.BindingContext.COLLECTION_LITERAL_CALL
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.getEffectiveExpectedType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.extractRadix
import java.math.BigInteger
import java.util.*

class ConstantExpressionEvaluator(
    internal val module: ModuleDescriptor,
    internal val languageVersionSettings: LanguageVersionSettings,
    project: Project
) {
    private val moduleAnnotationsResolver = ModuleAnnotationsResolver.getInstance(project)

    fun updateNumberType(
        numberType: KotlinType,
        expression: KtExpression?,
        statementFilter: StatementFilter,
        trace: BindingTrace
    ) {
        if (expression == null) return
        BindingContextUtils.updateRecordedType(numberType, expression, trace, false)

        if (expression !is KtConstantExpression) {
            val deparenthesized = KtPsiUtil.getLastElementDeparenthesized(expression, statementFilter)
            if (deparenthesized !== expression) {
                updateNumberType(numberType, deparenthesized, statementFilter, trace)
            }
            return
        }

        evaluateExpression(expression, trace, numberType)
    }

    internal fun resolveAnnotationArguments(
        resolvedCall: ResolvedCall<*>,
        trace: BindingTrace
    ): Map<Name, ConstantValue<*>> {
        val arguments = HashMap<Name, ConstantValue<*>>()
        for ((parameterDescriptor, resolvedArgument) in resolvedCall.valueArguments.entries) {
            val value = getAnnotationArgumentValue(trace, parameterDescriptor, resolvedArgument)
            if (value != null) {
                arguments.put(parameterDescriptor.name, value)
            }
        }
        return arguments
    }

    fun getAnnotationArgumentValue(
        trace: BindingTrace,
        parameterDescriptor: ValueParameterDescriptor,
        resolvedArgument: ResolvedValueArgument
    ): ConstantValue<*>? {
        val varargElementType = parameterDescriptor.varargElementType
        val argumentsAsVararg = varargElementType != null && !hasSpread(resolvedArgument)
        val constantType = if (argumentsAsVararg) varargElementType else parameterDescriptor.type
        val expectedType = getEffectiveExpectedType(parameterDescriptor, resolvedArgument, languageVersionSettings, trace)
        val compileTimeConstants = resolveAnnotationValueArguments(resolvedArgument, constantType!!, expectedType, trace)
        val constants = compileTimeConstants.map { it.toConstantValue(expectedType) }

        if (argumentsAsVararg) {
            if (isArrayPassedInNamedForm(constants, resolvedArgument)) return constants.single()

            if (parameterDescriptor.declaresDefaultValue() && compileTimeConstants.isEmpty()) return null

            return ConstantValueFactory.createArrayValue(constants, parameterDescriptor.type)
        } else {
            // we should actually get only one element, but just in case of getting many, we take the last one
            return constants.lastOrNull()
        }
    }

    private fun isArrayPassedInNamedForm(constants: List<ConstantValue<Any?>>, resolvedArgument: ResolvedValueArgument): Boolean {
        val constant = constants.singleOrNull() ?: return false
        val argument = resolvedArgument.arguments.singleOrNull() ?: return false
        return constant is ArrayValue && argument.isNamed()
    }

    private fun checkCompileTimeConstant(
        argumentExpression: KtExpression,
        expressionType: KotlinType,
        trace: BindingTrace,
        useDeprecationWarning: Boolean
    ) {
        val constant = ConstantExpressionEvaluator.getConstant(argumentExpression, trace.bindingContext)
        if (constant != null && constant.canBeUsedInAnnotations) {
            checkInnerPartsOfCompileTimeConstant(constant, trace, argumentExpression, useDeprecationWarning)
            return
        }

        val descriptor = expressionType.constructor.declarationDescriptor
        val diagnosticFactory = when {
            DescriptorUtils.isEnumClass(descriptor) -> Errors.ANNOTATION_ARGUMENT_MUST_BE_ENUM_CONST
            descriptor is ClassDescriptor && KotlinBuiltIns.isKClass(descriptor) -> Errors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL
            else -> Errors.ANNOTATION_ARGUMENT_MUST_BE_CONST
        }

        if (useDeprecationWarning)
            reportDeprecationWarningOnNonConst(argumentExpression, trace)
        else
            trace.report(diagnosticFactory.on(argumentExpression))
    }

    private fun checkInnerPartsOfCompileTimeConstant(
        constant: CompileTimeConstant<*>,
        trace: BindingTrace,
        argumentExpression: KtExpression,
        useDeprecationWarning: Boolean
    ) {
        // array(1, <!>null<!>, 3) - error should be reported on inner expression
        val callArguments = when (argumentExpression) {
            is KtCallExpression -> getArgumentExpressionsForArrayCall(argumentExpression, trace)
            is KtCollectionLiteralExpression -> getArgumentExpressionsForCollectionLiteralCall(argumentExpression, trace)
            else -> null
        }

        if (callArguments != null) {
            for (argument in callArguments) {
                val type = trace.getType(argument) ?: continue
                checkCompileTimeConstant(argument, type, trace, useDeprecationWarning)
            }
        }

        // TODO: Consider removing this check, because we already checked inner expression
        if (constant.usesNonConstValAsConstant) {
            if (useDeprecationWarning) {
                reportDeprecationWarningOnNonConst(argumentExpression, trace)
            } else {
                trace.report(Errors.NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION.on(argumentExpression))
            }

        }

        if (argumentExpression is KtClassLiteralExpression) {
            val lhsExpression = argumentExpression.receiverExpression
            if (lhsExpression != null) {
                val doubleColonLhs = trace.bindingContext.get(BindingContext.DOUBLE_COLON_LHS, lhsExpression)
                if (doubleColonLhs is DoubleColonLHS.Expression && !doubleColonLhs.isObjectQualifier) {
                    if (useDeprecationWarning) {
                        reportDeprecationWarningOnNonConst(argumentExpression, trace)
                    } else {
                        trace.report(Errors.ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL.on(argumentExpression))
                    }
                }
            }
        }
    }

    private fun reportDeprecationWarningOnNonConst(expression: KtExpression, trace: BindingTrace) {
        trace.report(Errors.ANNOTATION_ARGUMENT_IS_NON_CONST.on(expression))
    }

    private fun getArgumentExpressionsForArrayCall(
        expression: KtCallExpression,
        trace: BindingTrace
    ): List<KtExpression>? {
        val resolvedCall = expression.getResolvedCall(trace.bindingContext) ?: return null
        return getArgumentExpressionsForArrayLikeCall(resolvedCall)
    }

    private fun getArgumentExpressionsForCollectionLiteralCall(
        expression: KtCollectionLiteralExpression,
        trace: BindingTrace
    ): List<KtExpression>? {
        val resolvedCall = trace[COLLECTION_LITERAL_CALL, expression] ?: return null
        return getArgumentExpressionsForArrayLikeCall(resolvedCall)
    }

    private fun getArgumentExpressionsForArrayLikeCall(resolvedCall: ResolvedCall<*>): List<KtExpression>? {
        if (!CompileTimeConstantUtils.isArrayFunctionCall(resolvedCall)) {
            return null
        }

        val result = arrayListOf<KtExpression>()
        for ((_, resolvedValueArgument) in resolvedCall.valueArguments) {
            for (valueArgument in resolvedValueArgument.arguments) {
                val valueArgumentExpression = valueArgument.getArgumentExpression()
                if (valueArgumentExpression != null) {
                    result.add(valueArgumentExpression)
                }
            }
        }

        return result
    }

    private fun hasSpread(argument: ResolvedValueArgument): Boolean {
        val arguments = argument.arguments
        return arguments.size == 1 && arguments[0].getSpreadElement() != null
    }

    private fun resolveAnnotationValueArguments(
        resolvedValueArgument: ResolvedValueArgument,
        deprecatedExpectedType: KotlinType,
        expectedType: KotlinType,
        trace: BindingTrace
    ): List<CompileTimeConstant<*>> {
        val constants = ArrayList<CompileTimeConstant<*>>()
        for (argument in resolvedValueArgument.arguments) {
            val argumentExpression = argument.getArgumentExpression() ?: continue
            val constant = evaluateExpression(argumentExpression, trace, expectedType)
            if (constant is IntegerValueTypeConstant) {
                val defaultType = constant.getType(expectedType)
                updateNumberType(defaultType, argumentExpression, StatementFilter.NONE, trace)
            }
            if (constant != null) {
                constants.add(constant)
            }

            val expressionType = trace.getType(argumentExpression) ?: continue

            // this type check should not used as it can introduce subtle bugs when type checking rules against expected type are changing
            if (!languageVersionSettings.supportsFeature(LanguageFeature.ProhibitNonConstValuesAsVarargsInAnnotations) &&
                !KotlinTypeChecker.DEFAULT.isSubtypeOf(expressionType, deprecatedExpectedType)
            ) {
                if (KotlinTypeChecker.DEFAULT.isSubtypeOf(expressionType, expectedType)) {
                    checkCompileTimeConstant(argumentExpression, expressionType, trace, useDeprecationWarning = true)
                }

                continue // TYPE_MISMATCH should be reported otherwise
            }

            checkCompileTimeConstant(argumentExpression, expressionType, trace, useDeprecationWarning = false)
        }
        return constants
    }

    fun evaluateExpression(
        expression: KtExpression,
        trace: BindingTrace,
        expectedType: KotlinType? = TypeUtils.NO_EXPECTED_TYPE
    ): CompileTimeConstant<*>? {
        val visitor = ConstantExpressionEvaluatorVisitor(this, trace)
        val constant = visitor.evaluate(expression, expectedType) ?: return null

        checkExperimentalityOfConstantLiteral(expression, constant, expectedType, trace)

        return if (!constant.isError) constant else null
    }

    fun evaluateToConstantValue(
        expression: KtExpression,
        trace: BindingTrace,
        expectedType: KotlinType
    ): ConstantValue<*>? {
        return evaluateExpression(expression, trace, expectedType)?.toConstantValue(expectedType)
    }

    private fun checkExperimentalityOfConstantLiteral(
        expression: KtExpression,
        constant: CompileTimeConstant<*>,
        expectedType: KotlinType?,
        trace: BindingTrace
    ) {
        if (constant.isError) return
        if (!constant.parameters.isUnsignedNumberLiteral && !constant.parameters.isUnsignedLongNumberLiteral) return

        val constantType = when {
            constant is TypedCompileTimeConstant<*> -> constant.type
            expectedType != null -> constant.toConstantValue(expectedType).getType(module)
            else -> return
        }

        if (!UnsignedTypes.isUnsignedType(constantType)) return

        with(ExperimentalUsageChecker) {
            val descriptor = constantType.constructor.declarationDescriptor ?: return
            val experimentalities = descriptor.loadExperimentalities(moduleAnnotationsResolver, languageVersionSettings)

            reportNotAcceptedExperimentalities(
                experimentalities, expression, languageVersionSettings, trace, EXPERIMENTAL_UNSIGNED_LITERALS_DIAGNOSTICS
            )
        }
    }

    companion object {
        private val EXPERIMENTAL_UNSIGNED_LITERALS_DIAGNOSTICS = ExperimentalUsageChecker.ExperimentalityDiagnostics(
            Errors.EXPERIMENTAL_UNSIGNED_LITERALS, Errors.EXPERIMENTAL_UNSIGNED_LITERALS_ERROR
        )

        @JvmStatic
        fun getConstant(expression: KtExpression, bindingContext: BindingContext): CompileTimeConstant<*>? {
            val constant = getPossiblyErrorConstant(expression, bindingContext) ?: return null
            return if (!constant.isError) constant else null
        }

        @JvmStatic
        fun getPossiblyErrorConstant(expression: KtExpression, bindingContext: BindingContext): CompileTimeConstant<*>? {
            return bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expression)
        }
    }
}

private val DIVISION_OPERATION_NAMES =
    listOf(OperatorNameConventions.DIV, OperatorNameConventions.REM, OperatorNameConventions.MOD)
        .map(Name::asString)
        .toSet()

private class ConstantExpressionEvaluatorVisitor(
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val trace: BindingTrace
) : KtVisitor<CompileTimeConstant<*>?, KotlinType>() {
    private val builtIns = constantExpressionEvaluator.module.builtIns

    fun evaluate(expression: KtExpression, expectedType: KotlinType?): CompileTimeConstant<*>? {
        val recordedCompileTimeConstant = ConstantExpressionEvaluator.getPossiblyErrorConstant(expression, trace.bindingContext)
        if (recordedCompileTimeConstant != null) {
            return recordedCompileTimeConstant
        }

        val compileTimeConstant = expression.accept(this, expectedType ?: TypeUtils.NO_EXPECTED_TYPE)
        if (compileTimeConstant != null) {
            trace.record(BindingContext.COMPILE_TIME_VALUE, expression, compileTimeConstant)
            return compileTimeConstant
        }
        return null
    }

    private val stringExpressionEvaluator = object : KtVisitor<TypedCompileTimeConstant<String>, Nothing?>() {
        private fun createStringConstant(compileTimeConstant: CompileTimeConstant<*>): TypedCompileTimeConstant<String>? {
            val constantValue = compileTimeConstant.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)
            if (constantValue.isStandaloneOnlyConstant()) {
                return null
            }
            return when (constantValue) {
                is ErrorValue, is EnumValue -> return null
                is NullValue -> StringValue("null")
                else -> StringValue(constantValue.stringTemplateValue())
            }.wrap(compileTimeConstant.parameters)
        }

        fun evaluate(entry: KtStringTemplateEntry): TypedCompileTimeConstant<String>? {
            return entry.accept(this, null)
        }

        override fun visitStringTemplateEntryWithExpression(
            entry: KtStringTemplateEntryWithExpression,
            data: Nothing?
        ): TypedCompileTimeConstant<String>? {
            val expression = entry.expression ?: return null

            return evaluate(expression, builtIns.stringType)?.let {
                createStringConstant(it)
            }
        }

        override fun visitLiteralStringTemplateEntry(
            entry: KtLiteralStringTemplateEntry,
            data: Nothing?
        ): TypedCompileTimeConstant<String> =
            StringValue(entry.text).wrap()

        override fun visitEscapeStringTemplateEntry(entry: KtEscapeStringTemplateEntry, data: Nothing?): TypedCompileTimeConstant<String> =
            StringValue(entry.unescapedValue).wrap()
    }

    override fun visitConstantExpression(expression: KtConstantExpression, expectedType: KotlinType?): CompileTimeConstant<*>? {
        val text = expression.text ?: return null

        val nodeElementType = expression.node.elementType
        if (nodeElementType == KtNodeTypes.NULL) return NullValue().wrap()

        val result: Any? = when (nodeElementType) {
            KtNodeTypes.INTEGER_CONSTANT, KtNodeTypes.FLOAT_CONSTANT -> parseNumericLiteral(text, nodeElementType)
            KtNodeTypes.BOOLEAN_CONSTANT -> parseBoolean(text)
            KtNodeTypes.CHARACTER_CONSTANT -> CompileTimeConstantChecker.parseChar(expression)
            else -> throw IllegalArgumentException("Unsupported constant: " + expression)
        } ?: return null

        if (result is Double) {
            if (result.isInfinite()) {
                trace.report(Errors.FLOAT_LITERAL_CONFORMS_INFINITY.on(expression))
            }
            if (result == 0.0 && !TypeConversionUtil.isFPZero(text)) {
                trace.report(Errors.FLOAT_LITERAL_CONFORMS_ZERO.on(expression))
            }
        }

        if (result is Float) {
            if (result.isInfinite()) {
                trace.report(Errors.FLOAT_LITERAL_CONFORMS_INFINITY.on(expression))
            }
            if (result == 0.0f && !TypeConversionUtil.isFPZero(text)) {
                trace.report(Errors.FLOAT_LITERAL_CONFORMS_ZERO.on(expression))
            }
        }

        val isIntegerConstant = nodeElementType == KtNodeTypes.INTEGER_CONSTANT
        val isUnsignedLong = isIntegerConstant && hasUnsignedLongSuffix(text)
        val isUnsigned = isUnsignedLong || hasUnsignedSuffix(text)
        val isTyped = isUnsigned || hasLongSuffix(text)

        return createConstant(
            result,
            expectedType,
            CompileTimeConstant.Parameters(
                canBeUsedInAnnotation = true,
                isPure = !isTyped,
                isUnsignedNumberLiteral = isUnsigned,
                isUnsignedLongNumberLiteral = isUnsignedLong,
                usesVariableAsConstant = false,
                usesNonConstValAsConstant = false,
                isConvertableConstVal = false
            )
        )
    }

    override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, expectedType: KotlinType?): CompileTimeConstant<*>? {
        val deparenthesizedExpression = KtPsiUtil.deparenthesize(expression)
        if (deparenthesizedExpression != null && deparenthesizedExpression != expression) {
            return evaluate(deparenthesizedExpression, expectedType)
        }
        return null
    }

    override fun visitLabeledExpression(expression: KtLabeledExpression, expectedType: KotlinType?): CompileTimeConstant<*>? {
        val baseExpression = expression.baseExpression
        if (baseExpression != null) {
            return evaluate(baseExpression, expectedType)
        }
        return null
    }

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression, expectedType: KotlinType?): CompileTimeConstant<*>? {
        val sb = StringBuilder()
        var interupted = false
        var canBeUsedInAnnotation = true
        var usesVariableAsConstant = false
        var usesNonConstantVariableAsConstant = false
        for (entry in expression.entries) {
            val constant = stringExpressionEvaluator.evaluate(entry)
            if (constant == null) {
                interupted = true
                break
            } else {
                if (!constant.canBeUsedInAnnotations) canBeUsedInAnnotation = false
                if (constant.usesVariableAsConstant) usesVariableAsConstant = true
                if (constant.usesNonConstValAsConstant) usesNonConstantVariableAsConstant = true
                sb.append(constant.constantValue.value)
            }
        }
        return if (!interupted)
            createConstant(
                sb.toString(),
                expectedType,
                CompileTimeConstant.Parameters(
                    isPure = false,
                    isUnsignedNumberLiteral = false,
                    isUnsignedLongNumberLiteral = false,
                    canBeUsedInAnnotation = canBeUsedInAnnotation,
                    usesVariableAsConstant = usesVariableAsConstant,
                    usesNonConstValAsConstant = usesNonConstantVariableAsConstant,
                    isConvertableConstVal = false
                )
            )
        else null
    }

    private fun isStandaloneOnlyConstant(expression: KtExpression): Boolean {
        return ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.isStandaloneOnlyConstant() ?: return false
    }

    override fun visitBinaryWithTypeRHSExpression(
        expression: KtBinaryExpressionWithTypeRHS,
        expectedType: KotlinType?
    ): CompileTimeConstant<*>? {
        val compileTimeConstant = evaluate(expression.left, expectedType)
        if (compileTimeConstant != null) {
            if (expectedType != null && !TypeUtils.noExpectedType(expectedType)) {
                val constantType = when (compileTimeConstant) {
                    is TypedCompileTimeConstant<*> ->
                        compileTimeConstant.type
                    is IntegerValueTypeConstant ->
                        compileTimeConstant.getType(expectedType)
                    else ->
                        throw IllegalStateException("Unexpected compileTimeConstant class: ${compileTimeConstant::class.java.canonicalName}")

                }
                if (!constantType.isSubtypeOf(expectedType)) return null

            }
        }

        return compileTimeConstant
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, expectedType: KotlinType?): CompileTimeConstant<*>? {
        val leftExpression = expression.left ?: return null

        val operationToken = expression.operationToken
        if (OperatorConventions.BOOLEAN_OPERATIONS.containsKey(operationToken)) {
            val booleanType = builtIns.booleanType
            val leftConstant = evaluate(leftExpression, booleanType) ?: return null

            val rightExpression = expression.right ?: return null

            val rightConstant = evaluate(rightExpression, booleanType) ?: return null

            val leftValue = leftConstant.getValue(booleanType)
            val rightValue = rightConstant.getValue(booleanType)

            if (leftValue !is Boolean || rightValue !is Boolean) return null
            val result = when (operationToken) {
                KtTokens.ANDAND -> leftValue && rightValue
                KtTokens.OROR -> leftValue || rightValue
                else -> throw IllegalArgumentException("Unknown boolean operation token $operationToken")
            }
            return createConstant(
                result, expectedType,
                CompileTimeConstant.Parameters(
                    canBeUsedInAnnotation = true,
                    isPure = false,
                    isUnsignedNumberLiteral = false,
                    isUnsignedLongNumberLiteral = false,
                    usesVariableAsConstant = leftConstant.usesVariableAsConstant || rightConstant.usesVariableAsConstant,
                    usesNonConstValAsConstant = leftConstant.usesNonConstValAsConstant || rightConstant.usesNonConstValAsConstant,
                    isConvertableConstVal = false
                )
            )
        } else {
            return evaluateCall(expression.operationReference, leftExpression, expectedType)
        }
    }

    override fun visitCollectionLiteralExpression(
        expression: KtCollectionLiteralExpression,
        expectedType: KotlinType?
    ): CompileTimeConstant<*>? {
        val resolvedCall = trace.bindingContext[COLLECTION_LITERAL_CALL, expression] ?: return null
        return createConstantValueForArrayFunctionCall(resolvedCall)
    }

    private fun evaluateCall(
        callExpression: KtExpression,
        receiverExpression: KtExpression,
        expectedType: KotlinType?
    ): CompileTimeConstant<*>? {
        val resolvedCall = callExpression.getResolvedCall(trace.bindingContext) ?: return null
        if (!KotlinBuiltIns.isUnderKotlinPackage(resolvedCall.resultingDescriptor)) return null

        val resultingDescriptorName = resolvedCall.resultingDescriptor.name

        val argumentForReceiver = createOperationArgumentForReceiver(resolvedCall, receiverExpression) ?: return null
        if (isStandaloneOnlyConstant(argumentForReceiver.expression)) {
            return null
        }

        val argumentsEntrySet = resolvedCall.valueArguments.entries
        if (argumentsEntrySet.isEmpty()) {
            val result = evaluateUnaryAndCheck(argumentForReceiver, resultingDescriptorName.asString(), callExpression) ?: return null

            val isArgumentPure = isPureConstant(argumentForReceiver.expression)
            val canBeUsedInAnnotation = canBeUsedInAnnotation(argumentForReceiver.expression)
            val usesVariableAsConstant = usesVariableAsConstant(argumentForReceiver.expression)
            val usesNonConstValAsConstant = usesNonConstValAsConstant(argumentForReceiver.expression)
            val isNumberConversionMethod = resultingDescriptorName in OperatorConventions.NUMBER_CONVERSIONS
            return createConstant(
                result,
                expectedType,
                CompileTimeConstant.Parameters(
                    canBeUsedInAnnotation,
                    !isNumberConversionMethod && isArgumentPure,
                    false, false,
                    usesVariableAsConstant, usesNonConstValAsConstant,
                    false
                )
            )
        } else if (argumentsEntrySet.size == 1) {
            val (parameter, argument) = argumentsEntrySet.first()
            val argumentForParameter = createOperationArgumentForFirstParameter(argument, parameter) ?: return null
            if (isStandaloneOnlyConstant(argumentForParameter.expression)) {
                return null
            }

            if (isDivisionByZero(resultingDescriptorName.asString(), argumentForParameter.value)) {
                val parentExpression: KtExpression = PsiTreeUtil.getParentOfType(receiverExpression, KtExpression::class.java)!!
                trace.report(Errors.DIVISION_BY_ZERO.on(parentExpression))

                if ((isIntegerType(argumentForReceiver.value) && isIntegerType(argumentForParameter.value)) ||
                    !constantExpressionEvaluator.languageVersionSettings.supportsFeature(LanguageFeature.DivisionByZeroInConstantExpressions)) {
                    return ErrorValue.create("Division by zero").wrap()
                }
            }

            val result =
                evaluateBinaryAndCheck(argumentForReceiver, argumentForParameter, resultingDescriptorName.asString(), callExpression)
                        ?: return null

            val areArgumentsPure = isPureConstant(argumentForReceiver.expression) && isPureConstant(argumentForParameter.expression)
            val canBeUsedInAnnotation =
                canBeUsedInAnnotation(argumentForReceiver.expression) && canBeUsedInAnnotation(argumentForParameter.expression)
            val usesVariableAsConstant =
                usesVariableAsConstant(argumentForReceiver.expression) || usesVariableAsConstant(argumentForParameter.expression)
            val usesNonConstValAsConstant =
                usesNonConstValAsConstant(argumentForReceiver.expression) || usesNonConstValAsConstant(argumentForParameter.expression)
            val parameters = CompileTimeConstant.Parameters(
                canBeUsedInAnnotation, areArgumentsPure, false, false, usesVariableAsConstant, usesNonConstValAsConstant, false
            )
            return when (resultingDescriptorName) {
                OperatorNameConventions.COMPARE_TO -> createCompileTimeConstantForCompareTo(result, callExpression)?.wrap(parameters)
                OperatorNameConventions.EQUALS -> createCompileTimeConstantForEquals(result, callExpression)?.wrap(parameters)
                else -> {
                    createConstant(result, expectedType, parameters)
                }
            }
        }

        return null
    }

    private fun usesVariableAsConstant(expression: KtExpression) =
        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.usesVariableAsConstant ?: false

    private fun usesNonConstValAsConstant(expression: KtExpression) =
        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.usesNonConstValAsConstant ?: false

    private fun canBeUsedInAnnotation(expression: KtExpression) =
        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.canBeUsedInAnnotations ?: false

    private fun isPureConstant(expression: KtExpression) =
        ConstantExpressionEvaluator.getConstant(expression, trace.bindingContext)?.isPure ?: false

    private fun evaluateUnaryAndCheck(receiver: OperationArgument, name: String, callExpression: KtExpression): Any? {
        val functions = unaryOperations[UnaryOperationKey(receiver.ctcType, name)] ?: return null

        val (function, check) = functions
        val result = function(receiver.value)
        if (check == emptyUnaryFun) {
            return result
        }
        assert(isIntegerType(receiver.value)) { "Only integer constants should be checked for overflow" }
        assert(name == "minus" || name == "unaryMinus") { "Only negation should be checked for overflow" }

        if (receiver.value == result && !isZero(receiver.value)) {
            trace.report(Errors.INTEGER_OVERFLOW.on(callExpression.getStrictParentOfType<KtExpression>() ?: callExpression))
        }
        return result
    }

    private fun evaluateBinaryAndCheck(
        receiver: OperationArgument,
        parameter: OperationArgument,
        name: String,
        callExpression: KtExpression
    ): Any? {
        val functions = getBinaryOperation(receiver, parameter, name) ?: return null

        val (function, checker) = functions
        val actualResult = try {
            function(receiver.value, parameter.value)
        } catch (e: Exception) {
            null
        }
        if (checker == emptyBinaryFun) {
            return actualResult
        }
        assert(isIntegerType(receiver.value) && isIntegerType(parameter.value)) { "Only integer constants should be checked for overflow" }

        fun toBigInteger(value: Any?) = BigInteger.valueOf((value as Number).toLong())

        val refinedChecker = if (name == OperatorNameConventions.MOD.asString()) {
            getBinaryOperation(receiver, parameter, OperatorNameConventions.REM.asString())?.second ?: return null
        } else {
            checker
        }

        val resultInBigIntegers = refinedChecker(toBigInteger(receiver.value), toBigInteger(parameter.value))

        if (toBigInteger(actualResult) != resultInBigIntegers) {
            trace.report(Errors.INTEGER_OVERFLOW.on(callExpression.getStrictParentOfType<KtExpression>() ?: callExpression))
        }
        return actualResult
    }

    private fun getBinaryOperation(receiver: OperationArgument, parameter: OperationArgument, name: String) =
        binaryOperations[BinaryOperationKey(receiver.ctcType, parameter.ctcType, name)]

    private fun isDivisionByZero(name: String, parameter: Any?): Boolean {
        return name in DIVISION_OPERATION_NAMES && isZero(parameter)
    }

    private fun isZero(value: Any?): Boolean {
        return when {
            isIntegerType(value) -> (value as Number).toLong() == 0L
            value is Float || value is Double -> (value as Number).toDouble() == 0.0
            else -> false
        }
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression, expectedType: KotlinType?): CompileTimeConstant<*>? {
        val leftExpression = expression.baseExpression ?: return null

        return evaluateCall(expression.operationReference, leftExpression, expectedType)
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, expectedType: KotlinType?): CompileTimeConstant<*>? {
        val enumDescriptor = trace.bindingContext.get(BindingContext.REFERENCE_TARGET, expression)
        if (enumDescriptor != null && DescriptorUtils.isEnumEntry(enumDescriptor)) {
            val enumClassId = (enumDescriptor.containingDeclaration as ClassDescriptor).classId ?: return null
            return EnumValue(enumClassId, enumDescriptor.name).wrap()
        }

        val resolvedCall = expression.getResolvedCall(trace.bindingContext)
        if (resolvedCall != null) {
            val callableDescriptor = resolvedCall.resultingDescriptor
            if (callableDescriptor is VariableDescriptor) {
                // TODO: FIXME: see KT-10425
                if (callableDescriptor is PropertyDescriptor && callableDescriptor.modality != Modality.FINAL) return null

                val isConvertableConstVal =
                    callableDescriptor.isConst &&
                            ImplicitIntegerCoercion.isEnabledForConstVal(callableDescriptor) &&
                            callableDescriptor.compileTimeInitializer is IntValue

                return callableDescriptor.compileTimeInitializer?.wrap(
                    CompileTimeConstant.Parameters(
                        canBeUsedInAnnotation = isPropertyCompileTimeConstant(callableDescriptor),
                        isPure = false,
                        isUnsignedNumberLiteral = false,
                        isUnsignedLongNumberLiteral = false,
                        usesVariableAsConstant = true,
                        usesNonConstValAsConstant = !callableDescriptor.isConst,
                        isConvertableConstVal = isConvertableConstVal
                    )
                )
            }
        }
        return null
    }

    // TODO: Should be replaced with descriptor.isConst
    private fun isPropertyCompileTimeConstant(descriptor: VariableDescriptor): Boolean {
        if (descriptor.isVar) {
            return false
        }
        if (DescriptorUtils.isObject(descriptor.containingDeclaration) ||
            DescriptorUtils.isStaticDeclaration(descriptor)) {
            return descriptor.type.canBeUsedForConstVal()
        }
        return false
    }

    override fun visitQualifiedExpression(expression: KtQualifiedExpression, expectedType: KotlinType?): CompileTimeConstant<*>? {
        val selectorExpression = expression.selectorExpression
        // 1.toInt(); 1.plus(1);
        if (selectorExpression is KtCallExpression) {
            val qualifiedCallValue = evaluate(selectorExpression, expectedType)
            if (qualifiedCallValue != null) {
                return qualifiedCallValue
            }

            val calleeExpression = selectorExpression.calleeExpression
            if (calleeExpression !is KtSimpleNameExpression) {
                return null
            }

            val receiverExpression = expression.receiverExpression
            return evaluateCall(calleeExpression, receiverExpression, expectedType)
        }

        if (selectorExpression is KtSimpleNameExpression) {
            val result = evaluateCall(selectorExpression, expression.receiverExpression, expectedType)
            if (result != null) return result
        }

        // MyEnum.A, Integer.MAX_VALUE
        if (selectorExpression != null) {
            return evaluate(selectorExpression, expectedType)
        }

        return null
    }

    override fun visitCallExpression(expression: KtCallExpression, expectedType: KotlinType?): CompileTimeConstant<*>? {
        val call = expression.getResolvedCall(trace.bindingContext) ?: return null

        val resultingDescriptor = call.resultingDescriptor

        // arrayOf() or emptyArray()
        if (CompileTimeConstantUtils.isArrayFunctionCall(call)) {
            return createConstantValueForArrayFunctionCall(call)
        }

        // Ann()
        if (resultingDescriptor is ConstructorDescriptor) {
            val classDescriptor = resultingDescriptor.constructedClass
            return when {
                DescriptorUtils.isAnnotationClass(classDescriptor) -> {
                    val descriptor = AnnotationDescriptorImpl(
                        classDescriptor.defaultType,
                        constantExpressionEvaluator.resolveAnnotationArguments(call, trace),
                        SourceElement.NO_SOURCE
                    )
                    AnnotationValue(descriptor).wrap()
                }

                classDescriptor.isInlineClass() && UnsignedTypes.isUnsignedClass(classDescriptor) ->
                    createConstantValueForUnsignedTypeConstructor(call, resultingDescriptor, classDescriptor)

                else -> null
            }
        }

        return null
    }

    private fun createConstantValueForUnsignedTypeConstructor(
        call: ResolvedCall<*>,
        constructorDescriptor: ConstructorDescriptor,
        classDescriptor: ClassDescriptor
    ): TypedCompileTimeConstant<*>? {
        assert(classDescriptor.isInlineClass()) { "Unsigned type should be an inline class type, but it is: $classDescriptor" }

        if (!constructorDescriptor.isPrimary) return null

        val valueArguments = call.valueArguments
        if (valueArguments.size > 1) return null

        val underlyingType = classDescriptor.underlyingRepresentation()?.type ?: return null

        val argument = valueArguments.values.singleOrNull()?.arguments?.singleOrNull() ?: return null
        val argumentExpression = argument.getArgumentExpression() ?: return null

        val compileTimeConstant = evaluate(argumentExpression, underlyingType)
        val evaluatedArgument = compileTimeConstant?.toConstantValue(underlyingType) ?: return null

        val unsignedValue = ConstantValueFactory.createUnsignedValue(evaluatedArgument, classDescriptor.defaultType) ?: return null
        return unsignedValue.wrap(compileTimeConstant.parameters)
    }

    private fun createConstantValueForArrayFunctionCall(
        call: ResolvedCall<*>
    ): TypedCompileTimeConstant<List<ConstantValue<*>>>? {
        val returnType = call.resultingDescriptor.returnType ?: return null
        val componentType = builtIns.getArrayElementType(returnType)

        val arguments = call.valueArguments.values.flatMap { resolveArguments(it.arguments, componentType) }

        // not evaluated arguments are not constants: function-calls, properties with custom getter...
        val evaluatedArguments = arguments.filterNotNull()

        return ConstantValueFactory.createArrayValue(evaluatedArguments.map { it.toConstantValue(componentType) }, returnType)
            .wrap(
                usesVariableAsConstant = evaluatedArguments.any { it.usesVariableAsConstant },
                usesNonConstValAsConstant = arguments.any { it == null || it.usesNonConstValAsConstant }
            )
    }

    override fun visitClassLiteralExpression(expression: KtClassLiteralExpression, expectedType: KotlinType?): CompileTimeConstant<*>? {
        val type = trace.getType(expression)!!
        if (type.isError) return null
        val descriptor = type.constructor.declarationDescriptor
        if (descriptor !is ClassDescriptor || !KotlinBuiltIns.isKClass(descriptor)) return null
        return KClassValue.create(type.arguments.first().type)?.wrap()
    }

    private fun resolveArguments(valueArguments: List<ValueArgument>, expectedType: KotlinType): List<CompileTimeConstant<*>?> {
        val constants = arrayListOf<CompileTimeConstant<*>?>()
        for (argument in valueArguments) {
            val argumentExpression = argument.getArgumentExpression()
            if (argumentExpression != null) {
                constants.add(evaluate(argumentExpression, expectedType))
            }
        }
        return constants
    }

    override fun visitKtElement(element: KtElement, expectedType: KotlinType?): CompileTimeConstant<*>? {
        return null
    }

    private class OperationArgument(val value: Any, val ctcType: CompileTimeType<*>, val expression: KtExpression)

    private fun createOperationArgumentForReceiver(resolvedCall: ResolvedCall<*>, expression: KtExpression): OperationArgument? {
        val receiverExpressionType = getReceiverExpressionType(resolvedCall) ?: return null

        val receiverCompileTimeType = getCompileTimeType(receiverExpressionType) ?: return null

        return createOperationArgument(expression, receiverExpressionType, receiverCompileTimeType)
    }

    private fun createOperationArgumentForFirstParameter(
        argument: ResolvedValueArgument,
        parameter: ValueParameterDescriptor
    ): OperationArgument? {
        val argumentCompileTimeType = getCompileTimeType(parameter.type) ?: return null

        val arguments = argument.arguments
        if (arguments.size != 1) return null

        val argumentExpression = arguments.first().getArgumentExpression() ?: return null

        return createOperationArgument(argumentExpression, parameter.type, argumentCompileTimeType)
    }

    private fun getCompileTimeType(c: KotlinType): CompileTimeType<out Any>? =
        when (TypeUtils.makeNotNullable(c)) {
            builtIns.intType -> INT
            builtIns.byteType -> BYTE
            builtIns.shortType -> SHORT
            builtIns.longType -> LONG
            builtIns.doubleType -> DOUBLE
            builtIns.floatType -> FLOAT
            builtIns.charType -> CHAR
            builtIns.booleanType -> BOOLEAN
            builtIns.stringType -> STRING
            builtIns.anyType -> ANY
            else -> null
        }

    private fun createOperationArgument(
        expression: KtExpression,
        parameterType: KotlinType,
        compileTimeType: CompileTimeType<*>
    ): OperationArgument? {
        val compileTimeConstant = constantExpressionEvaluator.evaluateExpression(expression, trace, parameterType) ?: return null
        if (compileTimeConstant is TypedCompileTimeConstant && !compileTimeConstant.type.isSubtypeOf(parameterType)) return null
        val evaluationResult = compileTimeConstant.getValue(parameterType) ?: return null
        return OperationArgument(evaluationResult, compileTimeType, expression)
    }

    private fun createConstant(
        value: Any?,
        expectedType: KotlinType?,
        parameters: CompileTimeConstant.Parameters
    ): CompileTimeConstant<*>? {
        return if (parameters.isPure || parameters.isUnsignedNumberLiteral) {
            return createCompileTimeConstant(value, parameters, expectedType ?: TypeUtils.NO_EXPECTED_TYPE)
        } else {
            ConstantValueFactory.createConstantValue(value)?.wrap(parameters)
        }
    }

    private fun createCompileTimeConstant(
        value: Any?,
        parameters: CompileTimeConstant.Parameters,
        expectedType: KotlinType
    ): CompileTimeConstant<*>? {
        return when (value) {
            is Byte, is Short, is Int, is Long -> createIntegerCompileTimeConstant((value as Number).toLong(), parameters, expectedType)
            else -> ConstantValueFactory.createConstantValue(value)?.wrap(parameters)
        }
    }

    private fun createIntegerCompileTimeConstant(
        value: Long,
        parameters: CompileTimeConstant.Parameters,
        expectedType: KotlinType
    ): CompileTimeConstant<*>? {
        if (parameters.isUnsignedNumberLiteral &&
            !checkAccessibilityOfUnsignedTypes(
                constantExpressionEvaluator.module,
                constantExpressionEvaluator.languageVersionSettings
            )
        ) {
            return UnsignedErrorValueTypeConstant(value, parameters)
        }

        if (parameters.isUnsignedLongNumberLiteral) {
            return ULongValue(value).wrap(parameters)
        }

        if (TypeUtils.noExpectedType(expectedType) || expectedType.isError) {
            return createIntegerValueTypeConstant(value, constantExpressionEvaluator.module, parameters)
        }
        val integerValue = ConstantValueFactory.createIntegerConstantValue(
            value, expectedType, parameters.isUnsignedNumberLiteral
        )
        if (integerValue != null) {
            return integerValue.wrap(parameters)
        }

        return if (parameters.isUnsignedNumberLiteral) {
            when (value) {
                value.toInt().fromUIntToLong() -> UIntValue(value.toInt())
                else -> ULongValue(value)
            }
        } else {
            when (value) {
                value.toInt().toLong() -> IntValue(value.toInt())
                else -> LongValue(value)
            }
        }.wrap(parameters)
    }

    private fun checkAccessibilityOfUnsignedTypes(module: ModuleDescriptor, languageVersionSettings: LanguageVersionSettings): Boolean {
        val uInt = module.findClassAcrossModuleDependencies(KotlinBuiltIns.FQ_NAMES.uInt) ?: return false
        val accessibility = uInt.checkSinceKotlinVersionAccessibility(languageVersionSettings)
        // Case `NotAccessibleButWasExperimental` will be checked later in `checkExperimentalityOfConstantLiteral`
        return accessibility is SinceKotlinAccessibility.Accessible
    }

    private fun <T> ConstantValue<T>.wrap(parameters: CompileTimeConstant.Parameters): TypedCompileTimeConstant<T> =
        TypedCompileTimeConstant(this, constantExpressionEvaluator.module, parameters)

    private fun <T> ConstantValue<T>.wrap(
        canBeUsedInAnnotation: Boolean = this !is NullValue,
        isPure: Boolean = false,
        isUnsigned: Boolean = false,
        isUnsignedLong: Boolean = false,
        usesVariableAsConstant: Boolean = false,
        usesNonConstValAsConstant: Boolean = false,
        isConvertableConstVal: Boolean = false
    ): TypedCompileTimeConstant<T> =
        wrap(
            CompileTimeConstant.Parameters(
                canBeUsedInAnnotation,
                isPure,
                isUnsigned,
                isUnsignedLong,
                usesVariableAsConstant,
                usesNonConstValAsConstant,
                isConvertableConstVal
            )
        )
}

private fun hasLongSuffix(text: String) = text.endsWith('l') || text.endsWith('L')
private fun hasUnsignedSuffix(text: String) = text.endsWith('u') || text.endsWith('U')
private fun hasUnsignedLongSuffix(text: String) =
    text.endsWith("ul") || text.endsWith("uL") ||
            text.endsWith("Ul") || text.endsWith("UL")

private fun parseNumericLiteral(text: String, type: IElementType): Any? {
    val canonicalText = LiteralFormatUtil.removeUnderscores(text)
    return when (type) {
        KtNodeTypes.INTEGER_CONSTANT -> parseLong(canonicalText)
        KtNodeTypes.FLOAT_CONSTANT -> parseFloatingLiteral(canonicalText)
        else -> null
    }
}

private fun parseLong(text: String): Long? {
    fun String.removeSuffix(i: Int): String = this.substring(0, this.length - i)

    return try {
        val isUnsigned: Boolean
        val numberWithoutSuffix: String
        when {
            hasUnsignedLongSuffix(text) -> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(2)
            }
            hasUnsignedSuffix(text) -> {
                isUnsigned = true
                numberWithoutSuffix = text.removeSuffix(1)
            }
            hasLongSuffix(text) -> {
                isUnsigned = false
                numberWithoutSuffix = text.removeSuffix(1)
            }
            else -> {
                isUnsigned = false
                numberWithoutSuffix = text
            }
        }

        val (number, radix) = extractRadix(numberWithoutSuffix)

        if (isUnsigned) {
            java.lang.Long.parseUnsignedLong(number, radix)
        } else {
            java.lang.Long.parseLong(number, radix)
        }
    } catch (e: NumberFormatException) {
        null
    }
}

private fun parseFloatingLiteral(text: String): Any? {
    if (text.toLowerCase().endsWith('f')) {
        return parseFloat(text)
    }
    return parseDouble(text)
}

private fun parseDouble(text: String): Double? {
    try {
        return java.lang.Double.parseDouble(text)
    } catch (e: NumberFormatException) {
        return null
    }
}

private fun parseFloat(text: String): Float? {
    try {
        return java.lang.Float.parseFloat(text)
    } catch (e: NumberFormatException) {
        return null
    }
}

private fun parseBoolean(text: String): Boolean {
    if ("true".equals(text)) {
        return true
    } else if ("false".equals(text)) {
        return false
    }

    throw IllegalStateException("Must not happen. A boolean literal has text: " + text)
}


private fun createCompileTimeConstantForEquals(result: Any?, operationReference: KtExpression): ConstantValue<*>? {
    if (result is Boolean) {
        assert(operationReference is KtSimpleNameExpression) { "This method should be called only for equals operations" }
        val operationToken = (operationReference as KtSimpleNameExpression).getReferencedNameElementType()
        val value: Boolean = when (operationToken) {
            KtTokens.EQEQ -> result
            KtTokens.EXCLEQ -> !result
            KtTokens.IDENTIFIER -> {
                assert(operationReference.getReferencedNameAsName() == OperatorNameConventions.EQUALS) { "This method should be called only for equals operations" }
                result
            }
            else -> throw IllegalStateException("Unknown equals operation token: $operationToken ${operationReference.text}")
        }
        return BooleanValue(value)
    }
    return null
}

private fun createCompileTimeConstantForCompareTo(result: Any?, operationReference: KtExpression): ConstantValue<*>? {
    if (result is Int) {
        assert(operationReference is KtSimpleNameExpression) { "This method should be called only for compareTo operations" }
        val operationToken = (operationReference as KtSimpleNameExpression).getReferencedNameElementType()
        return when (operationToken) {
            KtTokens.LT -> BooleanValue(result < 0)
            KtTokens.LTEQ -> BooleanValue(result <= 0)
            KtTokens.GT -> BooleanValue(result > 0)
            KtTokens.GTEQ -> BooleanValue(result >= 0)
            KtTokens.IDENTIFIER -> {
                assert(operationReference.getReferencedNameAsName() == OperatorNameConventions.COMPARE_TO) { "This method should be called only for compareTo operations" }
                return IntValue(result)
            }
            else -> throw IllegalStateException("Unknown compareTo operation token: $operationToken")
        }
    }
    return null
}

fun isIntegerType(value: Any?) = value is Byte || value is Short || value is Int || value is Long

private fun getReceiverExpressionType(resolvedCall: ResolvedCall<*>): KotlinType? {
    return when (resolvedCall.explicitReceiverKind) {
        ExplicitReceiverKind.DISPATCH_RECEIVER -> resolvedCall.dispatchReceiver!!.type
        ExplicitReceiverKind.EXTENSION_RECEIVER -> resolvedCall.extensionReceiver!!.type
        ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> null
        ExplicitReceiverKind.BOTH_RECEIVERS -> null
        else -> null
    }
}

internal class CompileTimeType<T>(val name: String) {
    override fun toString() = name
}

internal val BYTE = CompileTimeType<Byte>("Byte")
internal val SHORT = CompileTimeType<Short>("Short")
internal val INT = CompileTimeType<Int>("Int")
internal val LONG = CompileTimeType<Long>("Long")
internal val DOUBLE = CompileTimeType<Double>("Double")
internal val FLOAT = CompileTimeType<Float>("Float")
internal val CHAR = CompileTimeType<Char>("Char")
internal val BOOLEAN = CompileTimeType<Boolean>("Boolean")
internal val STRING = CompileTimeType<String>("String")
internal val ANY = CompileTimeType<Any>("Any")

@Suppress("UNCHECKED_CAST")
internal fun <A, B> binaryOperation(
    a: CompileTimeType<A>,
    b: CompileTimeType<B>,
    functionName: String,
    operation: Function2<A, B, Any>,
    checker: Function2<BigInteger, BigInteger, BigInteger>
) = BinaryOperationKey(a, b, functionName) to Pair(
    operation,
    checker
) as Pair<Function2<Any?, Any?, Any>, Function2<BigInteger, BigInteger, BigInteger>>

@Suppress("UNCHECKED_CAST")
internal fun <A> unaryOperation(
    a: CompileTimeType<A>,
    functionName: String,
    operation: Function1<A, Any>,
    checker: Function1<Long, Long>
) = UnaryOperationKey(a, functionName) to Pair(operation, checker) as Pair<Function1<Any?, Any>, Function1<Long, Long>>

internal data class BinaryOperationKey<out A, out B>(val f: CompileTimeType<out A>, val s: CompileTimeType<out B>, val functionName: String)
internal data class UnaryOperationKey<out A>(val f: CompileTimeType<out A>, val functionName: String)

fun ConstantValue<*>.isStandaloneOnlyConstant(): Boolean {
    return this is KClassValue || this is EnumValue || this is AnnotationValue || this is ArrayValue
}

fun CompileTimeConstant<*>.isStandaloneOnlyConstant(): Boolean {
    return when (this) {
        is TypedCompileTimeConstant -> this.constantValue.isStandaloneOnlyConstant()
        else -> return false
    }
}
