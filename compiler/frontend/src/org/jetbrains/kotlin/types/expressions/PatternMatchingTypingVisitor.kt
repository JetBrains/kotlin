/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isBoolean
import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.RttiExpressionInformation
import org.jetbrains.kotlin.resolve.calls.checkers.RttiOperation
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT
import org.jetbrains.kotlin.resolve.calls.smartcasts.*
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.checkers.PrimitiveNumericComparisonCallChecker
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.*
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.createTypeInfo
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.noTypeInfo
import org.jetbrains.kotlin.types.typeUtil.containsError
import java.util.*

class PatternMatchingTypingVisitor internal constructor(facade: ExpressionTypingInternals) : ExpressionTypingVisitor(facade) {

    override fun visitIsExpression(expression: KtIsExpression, contextWithExpectedType: ExpressionTypingContext): KotlinTypeInfo {
        val context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT)
        val leftHandSide = expression.leftHandSide
        val typeInfo = facade.safeGetTypeInfo(leftHandSide, context)
        val knownType = typeInfo.type
        val typeReference = expression.typeReference
        if (typeReference != null && knownType != null) {
            val dataFlowValue = components.dataFlowValueFactory.createDataFlowValue(leftHandSide, knownType, context)
            val conditionInfo = checkTypeForIs(context, expression, expression.isNegated, knownType, typeReference, dataFlowValue).thenInfo
            val newDataFlowInfo = conditionInfo.and(typeInfo.dataFlowInfo)
            context.trace.record(BindingContext.DATAFLOW_INFO_AFTER_CONDITION, expression, newDataFlowInfo)
        }

        val resultTypeInfo = components.dataFlowAnalyzer.checkType(
            typeInfo.replaceType(components.builtIns.booleanType),
            expression,
            contextWithExpectedType
        )

        if (typeReference != null) {
            val rhsType = context.trace[BindingContext.TYPE, typeReference]
            val rttiInformation = RttiExpressionInformation(
                subject = leftHandSide,
                sourceType = knownType,
                targetType = rhsType,
                operation = if (expression.isNegated) RttiOperation.NOT_IS else RttiOperation.IS
            )
            components.rttiExpressionCheckers.forEach {
                it.check(rttiInformation, expression, context.trace)
            }
        }

        return resultTypeInfo
    }

    override fun visitWhenExpression(expression: KtWhenExpression, context: ExpressionTypingContext) =
        visitWhenExpression(expression, context, false)

    private sealed class Subject(
            val element: KtElement?,
            val typeInfo: KotlinTypeInfo?,
            val scopeWithSubject: LexicalScope?,
            val type: KotlinType = typeInfo?.type ?: ErrorUtils.createErrorType("Unknown type")
    ) {

        protected abstract fun createDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: KotlinBuiltIns): DataFlowValue
        abstract fun makeValueArgument(): ValueArgument?
        abstract val valueExpression: KtExpression?

        private var _dataFlowValue: DataFlowValue? = null
        val dataFlowValue get() = _dataFlowValue!!

        fun initDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: KotlinBuiltIns) {
            _dataFlowValue = createDataFlowValue(contextAfterSubject, builtIns)
        }

        val dataFlowInfo get() = typeInfo?.dataFlowInfo

        val jumpOutPossible get() = typeInfo?.jumpOutPossible ?: false

        class Expression(
                val expression: KtExpression,
                typeInfo: KotlinTypeInfo
        ) : Subject(expression, typeInfo, null) {
            override fun createDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: KotlinBuiltIns) =
                    DataFlowValueFactory.createDataFlowValue(expression, type, contextAfterSubject)

            override fun makeValueArgument(): ValueArgument =
                    CallMaker.makeExternalValueArgument(expression)

            override val valueExpression: KtExpression
                get() = expression
        }


        class Variable(
                val variable: KtProperty,
                val descriptor: VariableDescriptor,
                typeInfo: KotlinTypeInfo,
                scopeWithSubject: LexicalScope
        ) : Subject(variable, typeInfo, scopeWithSubject) {
            private val initializer = variable.initializer!!

            override fun createDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: KotlinBuiltIns) =
                    DataFlowValue(
                            IdentifierInfo.Variable(
                                    descriptor,
                                    DataFlowValue.Kind.STABLE_VALUE,
                                    contextAfterSubject.trace.bindingContext[BindingContext.BOUND_INITIALIZER_VALUE, descriptor]
                            ),
                            descriptor.type
                    )

            override fun makeValueArgument(): ValueArgument =
                    CallMaker.makeExternalValueArgument(
                            KtPsiFactory(variable.project, true).createExpression(variable.name!!),
                            initializer
                    )

            override val valueExpression: KtExpression
                get() = initializer
        }


        class None : Subject(null, null, null) {
            override fun createDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: KotlinBuiltIns) =
                    DataFlowValue.nullValue(builtIns)

            override fun makeValueArgument(): ValueArgument? = null

            override val valueExpression: KtExpression? get() = null
        }


        class Error(
                element: KtElement?,
                typeInfo: KotlinTypeInfo?,
                scopeWithSubject: LexicalScope?
        ) : Subject(element, typeInfo, scopeWithSubject) {
            override fun createDataFlowValue(contextAfterSubject: ExpressionTypingContext, builtIns: KotlinBuiltIns): DataFlowValue =
                    DataFlowValue.nullValue(builtIns)

            override fun makeValueArgument(): ValueArgument? = null

            override val valueExpression: KtExpression? get() = null
        }
    }

    fun visitWhenExpression(
        expression: KtWhenExpression,
        contextWithExpectedType: ExpressionTypingContext,
        @Suppress("UNUSED_PARAMETER") isStatement: Boolean
    ): KotlinTypeInfo {
        val trace = contextWithExpectedType.trace
        WhenChecker.checkDeprecatedWhenSyntax(trace, expression)
        WhenChecker.checkReservedPrefix(trace, expression)

        components.dataFlowAnalyzer.recordExpectedType(trace, expression, contextWithExpectedType.expectedType)

        val contextBeforeSubject = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT)

        // TODO change scope according to the bound value in the when header

        val subjectExpression = expression.subjectExpression
        val subjectVariable = expression.subjectVariable

        val subject = when {
            subjectVariable != null -> processVariableSubject(subjectVariable, contextBeforeSubject)
            subjectExpression != null -> Subject.Expression(subjectExpression, facade.getTypeInfo(subjectExpression, contextBeforeSubject))
            else -> Subject.None()
        }

        val contextAfterSubject = run {
            var result = contextBeforeSubject
            subject.scopeWithSubject?.let { result = result.replaceScope(it) }
            subject.dataFlowInfo?.let { result = result.replaceDataFlowInfo(it) }
            result
        }

        val contextWithExpectedTypeAndSubjectVariable =
                subject.scopeWithSubject?.let { contextWithExpectedType.replaceScope(it) } ?:
                contextWithExpectedType

//        val subjectType = subjectTypeInfo?.type ?: ErrorUtils.createErrorType("Unknown type")
//        val jumpOutPossibleInSubject: Boolean = subjectTypeInfo?.jumpOutPossible ?: false
//        val subjectDataFlowValue = subjectExpression?.let {
//            facade.components.dataFlowValueFactory.createDataFlowValue(it, subjectType, contextAfterSubject)
//        } ?: DataFlowValue.nullValue(components.builtIns)
        subject.initDataFlowValue(contextAfterSubject, components.builtIns)

        val possibleTypesForSubject = subjectTypeInfo?.dataFlowInfo?.getStableTypes(
            subjectDataFlowValue, components.languageVersionSettings
        ) ?: emptySet()
        checkSmartCastsInSubjectIfRequired(expression, contextBeforeSubject, subjectType, possibleTypesForSubject)
        val possibleTypesForSubject = subject.typeInfo?.dataFlowInfo?.getStableTypes(subject.dataFlowValue) ?: emptySet()
        checkSmartCastsInSubjectIfRequired(expression, contextBeforeSubject, subject.type, possibleTypesForSubject)

        val dataFlowInfoForEntries = analyzeConditionsInWhenEntries(expression, contextAfterSubject, subject)
        val whenReturnType = inferTypeForWhenExpression(expression, contextWithExpectedTypeAndSubjectVariable, contextAfterSubject, dataFlowInfoForEntries)
        val whenResultValue = whenReturnType?.let { facade.components.dataFlowValueFactory.createDataFlowValue(expression, it, contextAfterSubject) }

        val branchesTypeInfo =
                joinWhenExpressionBranches(expression, contextAfterSubject, whenReturnType, subject.jumpOutPossible, whenResultValue)

        val isExhaustive = WhenChecker.isWhenExhaustive(expression, trace)

        val branchesDataFlowInfo = branchesTypeInfo.dataFlowInfo
        val resultDataFlowInfo = if (expression.elseExpression == null && !isExhaustive) {
            // Without else expression in non-exhaustive when, we *must* take initial data flow info into account,
            // because data flow can bypass all when branches in this case
            branchesDataFlowInfo.or(contextAfterSubject.dataFlowInfo)
        } else {
            branchesDataFlowInfo
        }

        if (whenReturnType != null && isExhaustive && expression.elseExpression == null && KotlinBuiltIns.isNothing(whenReturnType)) {
            trace.record(BindingContext.IMPLICIT_EXHAUSTIVE_WHEN, expression)
        }

        val branchesType = branchesTypeInfo.type ?: return noTypeInfo(resultDataFlowInfo)
        val resultType = components.dataFlowAnalyzer.checkType(branchesType, expression, contextWithExpectedType)

        return createTypeInfo(resultType, resultDataFlowInfo, branchesTypeInfo.jumpOutPossible, contextWithExpectedType.dataFlowInfo)
    }

    private fun processVariableSubject(subjectVariable: KtProperty, contextBeforeSubject: ExpressionTypingContext): Subject {
        val trace = contextBeforeSubject.trace

        var hasIllegalDeclarationInSubject = false
        if (!components.languageVersionSettings.supportsFeature(LanguageFeature.VariableDeclarationInWhenSubject)) {
            trace.report(UNSUPPORTED_FEATURE.on(
                    subjectVariable,
                    Pair(LanguageFeature.VariableDeclarationInWhenSubject, components.languageVersionSettings)
            ))
        }
        else {
            val illegalDeclarationString = when {
                subjectVariable.isVar -> "var"
                subjectVariable.initializer == null -> "variable without initializer"
                subjectVariable.hasDelegateExpression() -> "delegated property"
                subjectVariable.getter != null || subjectVariable.setter != null -> "property with accessors"
                else -> null
            }

            if (illegalDeclarationString != null) {
                hasIllegalDeclarationInSubject = true
                trace.report(Errors.ILLEGAL_DECLARATION_IN_WHEN_SUBJECT.on(subjectVariable, illegalDeclarationString))
            }
        }

        val scopeWithSubjectVariable = ExpressionTypingUtils.newWritableScopeImpl(contextBeforeSubject, LexicalScopeKind.WHEN, components.overloadChecker)

        // Destructuring causes SOE in UAST :(
        val resolveResult = components.localVariableResolver.process(subjectVariable, contextBeforeSubject, contextBeforeSubject.scope, facade)
        val typeInfo = resolveResult.first
        val descriptor = resolveResult.second

        scopeWithSubjectVariable.addVariableDescriptor(descriptor)

        // NB typeInfo returned by 'localVariableResolver.process(...)' treats local variable declaration as a statement,
        // so 'typeInfo' above it has type 'kotlin.Unit'.
        // Propagate declared variable type as a "subject expression" type.
        val subjectTypeInfo = typeInfo.replaceType(descriptor.type)
        return if (hasIllegalDeclarationInSubject)
            Subject.Error(subjectVariable, subjectTypeInfo, scopeWithSubjectVariable)
        else
            Subject.Variable(subjectVariable, descriptor, subjectTypeInfo, scopeWithSubjectVariable)
    }

    private fun inferTypeForWhenExpression(
        expression: KtWhenExpression,
        contextWithExpectedType: ExpressionTypingContext,
        contextAfterSubject: ExpressionTypingContext,
        dataFlowInfoForEntries: List<DataFlowInfo>
    ): KotlinType? {
        if (expression.entries.all { it.expression == null }) {
            return components.builtIns.unitType
        }

        val wrappedArgumentExpressions = wrapWhenEntryExpressionsAsSpecialCallArguments(expression)
        val callForWhen = createCallForSpecialConstruction(expression, expression, wrappedArgumentExpressions)
        val dataFlowInfoForArguments =
            createDataFlowInfoForArgumentsOfWhenCall(callForWhen, contextAfterSubject.dataFlowInfo, dataFlowInfoForEntries)

        val resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
            callForWhen, ResolveConstruct.WHEN,
            object : AbstractList<String>() {
                override fun get(index: Int): String = "entry$index"
                override val size: Int get() = wrappedArgumentExpressions.size
            },
            Collections.nCopies(wrappedArgumentExpressions.size, false),
            contextWithExpectedType, dataFlowInfoForArguments
        )

        return resolvedCall.resultingDescriptor.returnType
    }

    private fun wrapWhenEntryExpressionsAsSpecialCallArguments(expression: KtWhenExpression): List<KtExpression> {
        val psiFactory = KtPsiFactory(expression)
        return expression.entries.mapNotNull { whenEntry ->
            whenEntry.expression?.let { psiFactory.wrapInABlockWrapper(it) }
        }
    }

    private fun analyzeConditionsInWhenEntries(
            expression: KtWhenExpression,
            contextAfterSubject: ExpressionTypingContext,
            subject: Subject
    ): ArrayList<DataFlowInfo> {
        val argumentDataFlowInfos = ArrayList<DataFlowInfo>()
        var inputDataFlowInfo = contextAfterSubject.dataFlowInfo
        for (whenEntry in expression.entries) {
            val conditionsInfo = analyzeWhenEntryConditions(
                    whenEntry,
                    contextAfterSubject.replaceDataFlowInfo(inputDataFlowInfo),
                    subject
            )
            inputDataFlowInfo = inputDataFlowInfo.and(conditionsInfo.elseInfo)

            if (whenEntry.expression != null) {
                argumentDataFlowInfos.add(conditionsInfo.thenInfo)
            }
        }
        return argumentDataFlowInfos
    }

    private fun joinWhenExpressionBranches(
        expression: KtWhenExpression,
        contextAfterSubject: ExpressionTypingContext,
        resultType: KotlinType?,
        jumpOutPossibleInSubject: Boolean,
        whenResultValue: DataFlowValue?
    ): KotlinTypeInfo {
        val bindingContext = contextAfterSubject.trace.bindingContext

        var currentDataFlowInfo: DataFlowInfo? = null
        var jumpOutPossible = jumpOutPossibleInSubject
        var errorTypeExistInBranch = false
        for (whenEntry in expression.entries) {
            val entryExpression = whenEntry.expression ?: continue

            val entryTypeInfo = BindingContextUtils.getRecordedTypeInfo(entryExpression, bindingContext) ?: continue
            val entryType = entryTypeInfo.type
            if (entryType == null) {
                errorTypeExistInBranch = true
            }

            val entryDataFlowInfo =
                if (whenResultValue != null && entryType != null) {
                    val entryValue = facade.components.dataFlowValueFactory.createDataFlowValue(entryExpression, entryType, contextAfterSubject)
                    entryTypeInfo.dataFlowInfo.assign(whenResultValue, entryValue, components.languageVersionSettings)
                } else {
                    entryTypeInfo.dataFlowInfo
                }

            currentDataFlowInfo =
                    when {
                        entryType != null && KotlinBuiltIns.isNothing(entryType) -> currentDataFlowInfo
                        currentDataFlowInfo != null -> currentDataFlowInfo.or(entryDataFlowInfo)
                        else -> entryDataFlowInfo
                    }

            jumpOutPossible = jumpOutPossible or entryTypeInfo.jumpOutPossible
        }

        val resultDataFlowInfo = currentDataFlowInfo ?: contextAfterSubject.dataFlowInfo
        return if (resultType == null || errorTypeExistInBranch && KotlinBuiltIns.isNothing(resultType))
            noTypeInfo(resultDataFlowInfo)
        else
            createTypeInfo(resultType, resultDataFlowInfo, jumpOutPossible, resultDataFlowInfo)
    }

    private fun checkSmartCastsInSubjectIfRequired(
        expression: KtWhenExpression,
        contextBeforeSubject: ExpressionTypingContext,
        subjectType: KotlinType,
        possibleTypesForSubject: Set<KotlinType>
    ) {
        val subjectExpression = expression.subjectExpression ?: return
        for (possibleCastType in possibleTypesForSubject) {
            val possibleCastClass = possibleCastType.constructor.declarationDescriptor as? ClassDescriptor ?: continue
            if (possibleCastClass.kind == ClassKind.ENUM_CLASS || possibleCastClass.modality == Modality.SEALED) {
                if (checkSmartCastToExpectedTypeInSubject(
                        contextBeforeSubject, subjectExpression, subjectType,
                        possibleCastType
                    )
                ) {
                    return
                }
            }
        }
        val isNullableType = TypeUtils.isNullableType(subjectType)
        val bindingContext = contextBeforeSubject.trace.bindingContext
        if (isNullableType && !WhenChecker.containsNullCase(expression, bindingContext)) {
            val notNullableType = TypeUtils.makeNotNullable(subjectType)
            if (checkSmartCastToExpectedTypeInSubject(
                    contextBeforeSubject, subjectExpression, subjectType,
                    notNullableType
                )
            ) {
                return
            }
        }
    }

    private fun checkSmartCastToExpectedTypeInSubject(
        contextBeforeSubject: ExpressionTypingContext,
        subjectExpression: KtExpression,
        subjectType: KotlinType,
        expectedType: KotlinType
    ): Boolean {
        val trace = TemporaryBindingTrace.create(contextBeforeSubject.trace, "Temporary trace for when subject nullability")
        val subjectContext = contextBeforeSubject.replaceExpectedType(expectedType).replaceBindingTrace(trace)
        val castResult = facade.components.dataFlowAnalyzer.checkPossibleCast(
            subjectType, KtPsiUtil.safeDeparenthesize(subjectExpression), subjectContext
        )
        if (castResult != null && castResult.isCorrect) {
            trace.commit()
            return true
        }
        return false
    }

    private fun analyzeWhenEntryConditions(
            whenEntry: KtWhenEntry,
            context: ExpressionTypingContext,
            subject: Subject
    ): ConditionalDataFlowInfo {
        if (whenEntry.isElse) {
            return ConditionalDataFlowInfo(context.dataFlowInfo)
        }

        var entryInfo: ConditionalDataFlowInfo? = null
        var contextForCondition = context
        for (condition in whenEntry.conditions) {
            val conditionInfo = checkWhenCondition(subject, condition, contextForCondition)
            entryInfo = entryInfo?.let {
                ConditionalDataFlowInfo(it.thenInfo.or(conditionInfo.thenInfo), it.elseInfo.and(conditionInfo.elseInfo))
            } ?: conditionInfo

            contextForCondition = contextForCondition.replaceDataFlowInfo(conditionInfo.elseInfo)
        }

        return entryInfo ?: ConditionalDataFlowInfo(context.dataFlowInfo)
    }

    private fun checkWhenCondition(
            subject: Subject,
            condition: KtWhenCondition,
            context: ExpressionTypingContext
    ): ConditionalDataFlowInfo {
        var newDataFlowInfo = noChange(context)

        condition.accept(object : KtVisitorVoid() {
            override fun visitWhenConditionInRange(condition: KtWhenConditionInRange) {
                val rangeExpression = condition.rangeExpression ?: return

                if (subject is Subject.None) {
                    context.trace.report(EXPECTED_CONDITION.on(condition))
                    val dataFlowInfo = facade.getTypeInfo(rangeExpression, context).dataFlowInfo
                    newDataFlowInfo = ConditionalDataFlowInfo(dataFlowInfo)
                    return
                }

                val argumentForSubject = subject.makeValueArgument() ?: return
                val typeInfo = facade.checkInExpression(
                    condition, condition.operationReference,
                    argumentForSubject, rangeExpression, context
                )
                val dataFlowInfo = typeInfo.dataFlowInfo
                newDataFlowInfo = ConditionalDataFlowInfo(dataFlowInfo)
                val type = typeInfo.type
                if (type == null || !isBoolean(type)) {
                    context.trace.report(TYPE_MISMATCH_IN_RANGE.on(condition))
                }
            }

            override fun visitWhenConditionIsPattern(condition: KtWhenConditionIsPattern) {
                if (subject is Subject.None) {
                    context.trace.report(EXPECTED_CONDITION.on(condition))
                }

                val typeReference = condition.typeReference ?: return

                val result = checkTypeForIs(context, condition, condition.isNegated, subject.type, typeReference, subject.dataFlowValue)
                newDataFlowInfo = if (condition.isNegated) ConditionalDataFlowInfo(result.elseInfo, result.thenInfo) else result
                val rhsType = context.trace[BindingContext.TYPE, typeReference]

                if (subject !is Subject.None) {
                    val rttiInformation = RttiExpressionInformation(
                            subject = subject.element!!,
                            sourceType = subject.type,
                            targetType = rhsType,
                            operation = if (condition.isNegated) RttiOperation.NOT_IS else RttiOperation.IS
                    )
                    components.rttiExpressionCheckers.forEach {
                        it.check(rttiInformation, condition, context.trace)
                    }
                }

            }

            override fun visitWhenConditionWithExpression(condition: KtWhenConditionWithExpression) {
                val expression = condition.expression ?: return

                val basicDataFlowInfo = checkTypeForExpressionCondition(context, expression, subject.type, subject is Subject.None, subject.dataFlowValue)
                val moduleDescriptor = DescriptorUtils.getContainingModule(context.scope.ownerDescriptor)
                val dataFlowInfoFromES = components.effectSystem.getDataFlowInfoWhenEquals(subject.valueExpression, expression, context.trace, moduleDescriptor)
                newDataFlowInfo = basicDataFlowInfo.and(dataFlowInfoFromES)
            }

            override fun visitKtElement(element: KtElement) {
                context.trace.report(UNSUPPORTED.on(element, this::class.java.canonicalName))
            }
        })
        return newDataFlowInfo
    }

    private fun checkTypeForExpressionCondition(
        context: ExpressionTypingContext,
        expression: KtExpression,
        subjectType: KotlinType,
        subjectExpression: KtExpression?,
        subjectDataFlowValue: DataFlowValue
    ): ConditionalDataFlowInfo {

        var newContext = context
        val typeInfo = facade.getTypeInfo(expression, newContext)
        val type = typeInfo.type ?: return noChange(newContext)
        newContext = newContext.replaceDataFlowInfo(typeInfo.dataFlowInfo)

        if (subjectExpression == null) { // condition expected
            val booleanType = components.builtIns.booleanType
            val checkedTypeInfo = components.dataFlowAnalyzer.checkType(typeInfo, expression, newContext.replaceExpectedType(booleanType))
            if (KotlinTypeChecker.DEFAULT.equalTypes(booleanType, checkedTypeInfo.type ?: type)) {
                val ifInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(expression, true, newContext)
                val elseInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(expression, false, newContext)
                return ConditionalDataFlowInfo(ifInfo, elseInfo)
            }
            return noChange(newContext)
        }

        checkTypeCompatibility(newContext, type, subjectType, expression)
        val expressionDataFlowValue = facade.components.dataFlowValueFactory.createDataFlowValue(expression, type, newContext)

        val subjectStableTypes =
            listOf(subjectType) + context.dataFlowInfo.getStableTypes(subjectDataFlowValue, components.languageVersionSettings)
        val expressionStableTypes =
            listOf(type) + newContext.dataFlowInfo.getStableTypes(expressionDataFlowValue, components.languageVersionSettings)
        PrimitiveNumericComparisonCallChecker.inferPrimitiveNumericComparisonType(
            context.trace,
            subjectStableTypes,
            expressionStableTypes,
            expression
        )

        val result = noChange(newContext)
        return ConditionalDataFlowInfo(
            result.thenInfo.equate(
                subjectDataFlowValue, expressionDataFlowValue,
                identityEquals = facade.components.dataFlowAnalyzer.typeHasEqualsFromAny(subjectType, expression),
                languageVersionSettings = components.languageVersionSettings
            ),
            result.elseInfo.disequate(
                subjectDataFlowValue,
                expressionDataFlowValue,
                components.languageVersionSettings
            )
        )
    }

    private fun checkTypeForIs(
        context: ExpressionTypingContext,
        isCheck: KtElement,
        negated: Boolean,
        subjectType: KotlinType,
        typeReferenceAfterIs: KtTypeReference,
        subjectDataFlowValue: DataFlowValue
    ): ConditionalDataFlowInfo {
        val typeResolutionContext =
            TypeResolutionContext(context.scope, context.trace, true, /*allowBareTypes=*/ true, context.isDebuggerContext)
        val possiblyBareTarget = components.typeResolver.resolvePossiblyBareType(typeResolutionContext, typeReferenceAfterIs)
        val targetType = TypeReconstructionUtil.reconstructBareType(
            typeReferenceAfterIs,
            possiblyBareTarget,
            subjectType,
            context.trace,
            components.builtIns
        )

        if (targetType.isDynamic()) {
            context.trace.report(DYNAMIC_NOT_ALLOWED.on(typeReferenceAfterIs))
        }
        val targetDescriptor = TypeUtils.getClassDescriptor(targetType)
        if (targetDescriptor != null && DescriptorUtils.isEnumEntry(targetDescriptor)) {
            context.trace.report(IS_ENUM_ENTRY.on(typeReferenceAfterIs))
        }
        if (!subjectType.containsError() && !TypeUtils.isNullableType(subjectType) && targetType.isMarkedNullable) {
            val element = typeReferenceAfterIs.typeElement
            assert(element is KtNullableType) { "element must be instance of " + KtNullableType::class.java.name }
            context.trace.report(Errors.USELESS_NULLABLE_CHECK.on(element as KtNullableType))
        }
        checkTypeCompatibility(context, targetType, subjectType, typeReferenceAfterIs)

        detectRedundantIs(context, subjectType, targetType, isCheck, negated, subjectDataFlowValue)

        if (CastDiagnosticsUtil.isCastErased(subjectType, targetType, KotlinTypeChecker.DEFAULT)) {
            context.trace.report(Errors.CANNOT_CHECK_FOR_ERASED.on(typeReferenceAfterIs, targetType))
        }
        return context.dataFlowInfo.let {
            ConditionalDataFlowInfo(it.establishSubtyping(subjectDataFlowValue, targetType, components.languageVersionSettings), it)
        }
    }

    private fun detectRedundantIs(
        context: ExpressionTypingContext,
        subjectType: KotlinType,
        targetType: KotlinType,
        isCheck: KtElement,
        negated: Boolean,
        subjectDataFlowValue: DataFlowValue
    ) {
        if (subjectType.containsError() || targetType.containsError()) return

        val possibleTypes =
            DataFlowAnalyzer.getAllPossibleTypes(subjectType, context, subjectDataFlowValue, context.languageVersionSettings)
        if (CastDiagnosticsUtil.isRefinementUseless(possibleTypes, targetType, false)) {
            context.trace.report(Errors.USELESS_IS_CHECK.on(isCheck, !negated))
        }
    }

    private fun noChange(context: ExpressionTypingContext) = ConditionalDataFlowInfo(context.dataFlowInfo)

    /*
     * (a: SubjectType) is Type
     */
    private fun checkTypeCompatibility(
        context: ExpressionTypingContext,
        type: KotlinType,
        subjectType: KotlinType,
        reportErrorOn: KtElement
    ) {
        // TODO : Take smart casts into account?
        if (TypeIntersector.isIntersectionEmpty(type, subjectType)) {
            context.trace.report(INCOMPATIBLE_TYPES.on(reportErrorOn, type, subjectType))
            return
        }

        // check if the pattern is essentially a 'null' expression
        if (KotlinBuiltIns.isNullableNothing(type) && !TypeUtils.isNullableType(subjectType)) {
            context.trace.report(SENSELESS_NULL_IN_WHEN.on(reportErrorOn))
        }
    }
}
