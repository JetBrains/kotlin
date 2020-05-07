/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.builtins.extractParameterNameFromFunctionTypeArgument
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.ValueArgumentName
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.LambdaArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.checkers.UnderscoreUsageChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatchStatus
import org.jetbrains.kotlin.resolve.calls.model.ArgumentUnmapped
import org.jetbrains.kotlin.resolve.calls.model.DataFlowInfoForArgumentsImpl
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyImpl
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.PackageQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.TypeAliasQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.TypeParameterQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.expression
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findFirstFromMeAndParent
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.synthetic.isVisibleOutside
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeCheckerImpl
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.equalTypesOrNulls
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.util.Locale

/**
 * This class is used to resolve a KTX Element to the corresponding set of calls on the composer, and the individual calls for
 * each attribute, etc.
 */
class ComposeEmitResolver(
    callResolver: CallResolver,
    project: Project,
    private val composer: ComposerMetadata
) {

    private val callResolver = NoInterceptionCallResolver(callResolver)

    private class TempParameterInfo(
        val attribute: AttributeInfo,
        val descriptor: DeclarationDescriptor
    )

    // NOTE(lmr): I am unsure of the performance consequences of using KtPsiFactory. it appears to create a file for every
    // call, which seems like something we definitely do not want, but it's also used in resolving for(..) loops, so
    // maybe it's not terrible.
    private val psiFactory = KtPsiFactory(project, markGenerated = false)

    private fun KotlinType.isEmittable() = composer.isEmittable(this)

    private fun KotlinType.isCompoundEmittable() = composer.isCompoundEmittable(this)

    private fun isImplicitConstructorParam(
        param: ValueParameterDescriptor,
        fn: CallableDescriptor
    ) = composer.isImplicitConstructorParam(param, fn)

    fun resolveCandidates(
        call: Call,
        candidates: Collection<FunctionDescriptor>,
        name: Name,
        resolutionContext: ResolutionContext<*>
    ): List<ComposableEmitDescriptor> {
        val context = ExpressionTypingContext.newContext(
            resolutionContext.trace,
            resolutionContext.scope,
            resolutionContext.dataFlowInfo,
            resolutionContext.expectedType,
            resolutionContext.languageVersionSettings,
            resolutionContext.dataFlowValueFactory
        )
        val openTagExpr = call.calleeExpression ?: error("Expected calleeExpression")

        val tmpTraceAndCache = TemporaryTraceAndCache.create(
            context,
            "trace for ktx tag",
            openTagExpr
        )

        val contextToUse = context.replaceTraceAndCache(tmpTraceAndCache)

        val attrInfos = mutableMapOf<String, AttributeInfo>()

        for (arg in call.valueArguments) {
            if (arg is KtLambdaArgument) continue

            // NOTE: emits *must* use all named parameters. If they don't, we will not resolve them.
            val argName = arg.getArgumentName() ?: return emptyList()

            val key = argName.referenceExpression
            val value = arg.getArgumentExpression()
            val identifier = argName.asName.asString()

            // NOTE: We don't have to check for duplicate argument names, that will be done elsewhere.

            // NOTE: We don't have to deal with punning. punning isn't supported in FCS.

            attrInfos[identifier] = AttributeInfo(
                value = value ?: error("expected a value expression"),
                key = key,
                name = identifier
            )
        }

        for (arg in call.functionLiteralArguments) {
            if (attrInfos.containsKey(CHILDREN_KEY))
                error("Only one children argument supported at a time")
            attrInfos[CHILDREN_KEY] =
                AttributeInfo(
                    value = arg.getLambdaExpression() ?: error("expected a value expression"),
                    key = null,
                    name = CHILDREN_KEY
                )
        }

        val receiver = resolveReceiver(openTagExpr, contextToUse)

        attrInfos[TAG_KEY] =
            AttributeInfo(
                value = when (receiver) {
                    is ExpressionReceiver -> receiver.expression
                    else -> openTagExpr
                },
                key = null,
                name = TAG_KEY
            )

        val result = resolveChild(
            openTagExpr,
            candidates,
            name,
            makeCall(
                callElement = call.callElement,
                calleeExpression = call.calleeExpression,
                receiver = call.explicitReceiver,
                dispatchReceiver = call.dispatchReceiver
            ),
            attrInfos,
            context
        )

        tmpTraceAndCache.commit()
        return result
    }

    private fun resolveChild(
        expression: KtExpression,
        candidates: Collection<FunctionDescriptor>,
        name: Name,
        call: Call,
        attributes: Map<String, AttributeInfo>,
        context: ExpressionTypingContext
    ): List<ComposableEmitDescriptor> {
        return candidates
            .mapNotNull { result ->
            val tmpForCandidate = TemporaryTraceAndCache.create(
                context, "trace to resolve ktx element", expression
            )

            val trace: BindingTrace = tmpForCandidate.trace

            val candidateContext = context
                .replaceTraceAndCache(tmpForCandidate)
                .replaceBindingTrace(trace)

            val attrsUsedInCall = mutableSetOf<String>()

            val attrsUsedInSets = mutableSetOf<String>()

            val subMissingRequiredAttributes = mutableListOf<DeclarationDescriptor>()

            val usedAttributeInfos = mutableListOf<TempParameterInfo>()

            val candidateResults = resolveCandidate(
                result,
                call,
                attributes,
                attrsUsedInCall,
                usedAttributeInfos,
                subMissingRequiredAttributes,
                candidateContext
            )

            if (candidateResults.isNothing)
                return@mapNotNull null

            val resolvedCall = candidateResults.resultingCalls.first()

            if (!candidateResults.isSuccess) {
                when (candidateResults.resultCode) {
                    OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH -> {
                        resolvedCall.call.valueArguments.map {
                            resolvedCall.getArgumentMapping(it)
                        }.forEach {
                            when (it) {
                                is ArgumentMatch -> {
                                    when (it.status) {
                                        ArgumentMatchStatus.TYPE_MISMATCH -> {
                                            return@forEach
                                        }
                                        ArgumentMatchStatus.MATCH_MODULO_UNINFERRED_TYPES -> {
                                            return@forEach
                                        }
                                        ArgumentMatchStatus.UNKNOWN -> {
                                            // NOTE(lmr): This can happen with the implicit constructor params. ignore it
                                        }
                                        ArgumentMatchStatus.SUCCESS -> {
                                            // do nothing
                                        }
                                        ArgumentMatchStatus.ARGUMENT_HAS_NO_TYPE -> {
                                            // NOTE(lmr): This can happen when the attribute has no value expression, since
                                            // we pass in a fake psi node for the value in that case.
                                        }
                                    }
                                }
                                is ArgumentUnmapped -> {
                                    return@mapNotNull null
                                }
                            }
                        }
                    }
                    OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE,
                    OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES,
                    OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER -> {
                        return@mapNotNull null
                    }
                    else -> {
                        error("new kind of resolution problem. figure out why this happened...")
                    }
                }
            }

            if (
                !descriptorsEqualWithSubstitution(
                    resolvedCall.semanticCall.candidateDescriptor.original,
                    result.original
                )
            ) {
                // if this happens, we've likely resolved the candidate to one of the other candidates, and we don't want that.
                // we fail in this case, since it will succeed once we actually process the candidate that this resolved to.
                return@mapNotNull null
            }

            val returnType = resolvedCall.resultingDescriptor.returnType ?: builtIns.unitType

            if (!returnType.isEmittable()) return@mapNotNull null

            var hasChildren = false

            val emitCall = resolveComposerEmit(
                constructedType = returnType,
                hasBody = attributes.contains(CHILDREN_KEY),
                implicitCtorTypes = resolvedCall.call.valueArguments.mapNotNull {
                    (it as? ImplicitCtorValueArgument)?.type
                },
                expressionToReportErrorsOn = expression,
                context = candidateContext
            ) ?: return@mapNotNull null

            if (attributes.contains(CHILDREN_KEY) && returnType.isCompoundEmittable()) {
                attrsUsedInSets.add(CHILDREN_KEY)
                hasChildren = true
            }

            val updateReceiverScope = emitCall
                .resultingDescriptor
                .valueParameters
                .first { it.name == KtxNameConventions.EMIT_UPDATER_PARAMETER }
                .type
                .getReceiverTypeFromFunctionType() ?: error("Expected receiver type")

            val setterValidations = resolveAllSetAttributes(
                expressionToReportErrorsOn = expression,
                receiverScope = updateReceiverScope,
                type = returnType,
                attributes = attributes,
                attributesUsedInCall = attrsUsedInCall,
                consumedAttributes = attrsUsedInSets,
                missingRequiredAttributes = subMissingRequiredAttributes,
                context = candidateContext
            )

            val pivotals = resolvePivotalAttributes(
                attributes,
                attrsUsedInCall,
                usedAttributeInfos,
                setterValidations,
                returnType
            )

            return@mapNotNull ComposableEmitDescriptor.build(
                hasChildren = hasChildren,
                ctorCall = resolvedCall,
                validations = setterValidations,
                pivotals = pivotals,
                ctorParams = resolvedCall.buildParamsFromAttributes(attributes),
                emitCall = emitCall,
                name = name,
                composerMetadata = composer
            )
        }
    }

    private fun resolvePivotalAttributes(
        attributes: Map<String, AttributeInfo>,
        attrsUsedInCall: Set<String>,
        callParamInfos: List<TempParameterInfo>,
        validations: List<ValidatedAssignment>,
        returnType: KotlinType?
    ): List<String> {
        val result = mutableListOf<String>()

        if (returnType == null || returnType.isUnit()) {
            return emptyList()
        }

        val validationSet = validations.map { it.name }.toSet()

        // if you were in the ctor call but not in the sets, you *have* to be pivotal
        for (info in callParamInfos) {
            if (validationSet.contains(info.attribute.name)) continue
            attributes[info.attribute.name] ?: continue
            result.add(info.attribute.name)
        }

        // There are additional cases where attributes can be pivotal:
        //   1. It is annotated as @Pivotal
        //   2. It is a `val` ctor parameter
        for (assignment in validations) {
            val name = assignment.name
            val descriptor = assignment.descriptor

            if (descriptor is PropertyDescriptor &&
                attrsUsedInCall.contains(name) && !descriptor.isVar) {
                result.add(name)
                continue
            }
        }

        return result
    }

    private fun ResolvedCall<*>.buildParamsFromAttributes(
        attributes: Map<String, AttributeInfo>
    ): List<String> {
        val possbileChildrenParam = valueArguments.keys.possibleChildrenParameter
        return valueArguments.mapNotNull { (param, value) ->
            val name = param.name.asString()
//            var type = param.type
            var attr = attributes[name]

            if (param == possbileChildrenParam && attr == null) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    attr = childrenAttr
                }
//                if (param.isComposableFromChildrenAnnotation()) {
//                    type = type.makeComposable(module)
//                }
            }

            if (value is DefaultValueArgument) {
                return@mapNotNull null
            }

            if (attr == null && isImplicitConstructorParam(param, resultingDescriptor)) {
                return@mapNotNull null
//                ImplicitCtorValueNode(
//                    name = name,
//                    descriptor = param,
//                    type = type
//                )
            }

            if (attr == null) {
                error("Couldn't find attribute but expected to. param=$param name=$name")
            }

            attr.name
        }
    }

    private fun resolveAllSetAttributes(
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType,
        type: KotlinType?,
        attributes: Map<String, AttributeInfo>,
        attributesUsedInCall: Set<String>,
        consumedAttributes: MutableSet<String>,
        missingRequiredAttributes: MutableList<DeclarationDescriptor>,
        context: ExpressionTypingContext
    ): List<ValidatedAssignment> {
        if (type == null) return emptyList()
        val results = mutableListOf<ValidatedAssignment>()

        val tempForAttributes = TemporaryTraceAndCache.create(
            context, "temp for attributes", expressionToReportErrorsOn
        )

        val tempForValidations = TemporaryTraceAndCache.create(
            context, "temp for validations", expressionToReportErrorsOn
        )

        for ((name, attribute) in attributes) {
            if (name == TAG_KEY) continue
            if (name == CHILDREN_KEY) {
                continue
            }
            val keyExpr = attribute.key ?: error("key expected")

            val expectedTypes = mutableListOf<KotlinType>()

            var resolvedCall: ResolvedCall<*>? = null

            // NOTE(lmr): A ktx element that has access (like it's a recursive call or a nested class) to the private property
            // of the tag will be able to set it as an attribute...  I'm not sure if that'a s good thing or not, but unless we
            // do something extra, that is indeed possible. Perhaps it's something we should look into.

            if (resolvedCall == null) {
                resolvedCall = resolveAttributeAsSetter(
                    type,
                    attribute.name,
                    keyExpr,
                    attribute.value,
                    expectedTypes,
                    context.replaceTraceAndCache(tempForAttributes)
                )
            }

            if (resolvedCall == null) {
                resolvedCall = resolveAttributeAsProperty(
                    type,
                    keyExpr,
                    attribute.value,
                    expectedTypes,
                    context.replaceTraceAndCache(tempForAttributes)
                )
            }

            if (resolvedCall != null) {

                val validationType = when {
                    attributesUsedInCall.contains(name) -> ValidationType.UPDATE
                    else -> ValidationType.SET
                }

                val attrType = when (val descriptor = resolvedCall.resultingDescriptor) {
                    is FunctionDescriptor -> descriptor.valueParameters.firstOrNull()?.type
                    is PropertyDescriptor -> descriptor.type
                    else -> null
                } ?: continue

                val (validationCall, uncheckedValidationCall, lambdaDescriptor) =
                    resolveValidationCall(
                        expressionToReportErrorsOn = expressionToReportErrorsOn,
                        receiverScope = receiverScope,
                        assignmentReceiverScope = type,
                        validationType = validationType,
                        attrType = attrType,
                        context = context.replaceTraceAndCache(tempForValidations)
                    )

                results.add(
                    ValidatedAssignment(
                        validationType = validationType,
                        assignment = resolvedCall,
                        assignmentLambda = lambdaDescriptor,
                        name = name,
                        validationCall = validationCall,
                        uncheckedValidationCall = uncheckedValidationCall,
                        descriptor = resolvedCall.resultingDescriptor,
                        type = attrType
                    )
                )
                consumedAttributes.add(name)
            }
        }

        if (!type.isUnit()) {
            val cls = type.constructor.declarationDescriptor as? ClassDescriptor
                ?: error("unexpected classifier descriptor")
            val requiredAttributes = cls.unsubstitutedMemberScope
                .getContributedDescriptors()
                .mapNotNull { it as? PropertyDescriptor }
                // NOTE(lmr): I think we should consider not marking lateinit properties as required. It would maybe align
                // ourselves more with the language semantic of `lateinit`
                .filter {
                    it.isLateInit && it.visibility.isVisibleOutside() &&
                            !Visibilities.isPrivate(it.visibility)
                }

            requiredAttributes
                .filter { !consumedAttributes.contains(it.name.asString()) }
                .ifNotEmpty { missingRequiredAttributes.addAll(this) }
        }

        tempForAttributes.commit()
        tempForValidations.commit()
        return results
    }

    private fun resolveAttributeAsSetter(
        instanceType: KotlinType,
        name: String,
        keyExpr: KtReferenceExpression,
        valueExpr: KtExpression,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val setterName = Name.identifier(
            ComposeUtils.setterMethodFromPropertyName(
                name
            )
        )
        val receiver = TransientReceiver(instanceType)

        val call = makeCall(
            keyExpr,
            calleeExpression = keyExpr,
            valueArguments = listOf(CallMaker.makeValueArgument(valueExpr)),
            receiver = receiver
        )

        val temporaryForFunction = TemporaryTraceAndCache.create(
            context, "trace to resolve as function call", keyExpr
        )

        val results = callResolver.computeTasksAndResolveCall<FunctionDescriptor>(
            BasicCallResolutionContext.create(
                context.replaceTraceAndCache(temporaryForFunction),
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
            ),
            setterName,
            keyExpr,
            NewResolutionOldInference.ResolutionKind.Function
        )

        if (results.isNothing) {
            return null
        }

        if (results.isAmbiguity || temporaryForFunction.trace.hasTypeMismatchErrorsOn(valueExpr)) {
            expectedTypes.addAll(
                results.resultingCalls.mapNotNull {
                    it.resultingDescriptor.valueParameters.firstOrNull()
                }.map { it.type }
            )
            return null
        }

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context)
            ?: return null

        temporaryForFunction.commit()

        return resolvedCall
    }

    private fun resolveAttributeAsProperty(
        instanceType: KotlinType,
        keyExpr: KtSimpleNameExpression,
        valueExpr: KtExpression,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {

        // NOTE(lmr): I'm not sure what the consequences are of using the tagExpr as the receiver...
        val receiver = TransientReceiver(instanceType)

        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve as local variable or property", keyExpr
        )

        val contextToUse = context.replaceTraceAndCache(temporaryForVariable)

        val call = CallMaker.makePropertyCall(receiver, null, keyExpr)

        val contextForVariable = BasicCallResolutionContext.create(
            contextToUse,
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
        )

        val results = callResolver.resolveSimpleProperty(contextForVariable)

        if (results.isNothing) {
            return null
        }

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, contextToUse)
            ?: return null

        val descriptor = resolvedCall.resultingDescriptor as PropertyDescriptor
        val expectedType = descriptor.type

        if (temporaryForVariable.trace.hasTypeMismatchErrorsOn(valueExpr)) {
            expectedTypes.add(expectedType)
            return null
        }

        if (descriptor.setter == null) {
            // NOTE(lmr): we probably shouldn't do this. if this ends up being a cross-module compile, then the
            // setter will be gone. I'm not sure the right way to figure out whether or not a property is
            // settable or not...
            return null
        }

        temporaryForVariable.commit()

        return resolvedCall
    }

    private fun resolveCandidate(
        candidate: FunctionDescriptor,
        original: Call,
        attributes: Map<String, AttributeInfo>,
        usedAttributes: MutableSet<String>,
        usedAttributeInfos: MutableList<TempParameterInfo>,
        missingRequiredAttributes: MutableList<DeclarationDescriptor>,
        context: ExpressionTypingContext
    ): OverloadResolutionResults<FunctionDescriptor> {
        val valueArguments = mutableListOf<ValueArgument>()

        val dispatchReceiver = original.dispatchReceiver

        val stableParamNames = candidate.hasStableParameterNames()

        val possibleChildrenParameter =
            candidate.valueParameters.possibleChildrenParameter

        for (param in candidate.valueParameters) {
            val name = param.name.asString()
            val attr = attributes[name]
            var arg: ValueArgument? = null

            if (arg == null && param == possibleChildrenParameter) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    usedAttributes.add(CHILDREN_KEY)

                    usedAttributeInfos.add(
                        TempParameterInfo(
                            attribute = childrenAttr,
                            descriptor = param
                        )
                    )

                    arg = childrenAttr.toValueArgument(name, stableParamNames)
                }
            }

            if (arg == null && attr != null) {
                usedAttributes.add(name)
                usedAttributeInfos.add(
                    TempParameterInfo(
                        attribute = attr,
                        descriptor = param
                    )
                )
                context.trace.record(BindingContext.REFERENCE_TARGET, attr.key, param)
                arg = attr.toValueArgument(attr.name, stableParamNames)
            }

            if (arg == null && isImplicitConstructorParam(param, candidate)) {
                arg = ImplicitCtorValueArgument(param.type)
            }

            if (arg != null) {
                valueArguments.add(arg)
            } else if (!param.declaresDefaultValue()) {
                // missing required parameter!
                if (
                    dispatchReceiver?.type?.isExtensionFunctionType != true ||
                    param != candidate.valueParameters[0]
                ) {
                    // if this isn't the case, the missing parameter is an extension parameter...
                    // and it will be provided implicitly. We don't want to diagnose this as a
                    // missing attribute. If it is missing, the call resolver will add a
                    // diagnostic anyway.
                    missingRequiredAttributes.add(param)
                }
            }
        }

        val call = makeCall(
            original.callElement,
            valueArguments = valueArguments,
            calleeExpression = original.calleeExpression,
            receiver = original.explicitReceiver,
            dispatchReceiver = dispatchReceiver
        )

        val contextForVariable = BasicCallResolutionContext.create(
            context,
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
        )

        return callResolver.resolveFunctionCall(contextForVariable)
    }

    private fun resolveReceiver(
        expression: KtExpression,
        context: ExpressionTypingContext
    ): Receiver? {
        if (expression !is KtQualifiedExpression) return null
        val currentContext = context
            .replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        expression.elementChain(currentContext)

        val receiverExpr = expression.receiverExpression

        return context.trace.get(BindingContext.QUALIFIER, receiverExpr)
            ?: ExpressionReceiver.create(
                receiverExpr,
                ErrorUtils.createErrorType("Type for " + receiverExpr.text),
                context.trace.bindingContext
            )
    }

    private fun makeValueArgument(
        type: KotlinType,
        context: ExpressionTypingContext,
        forceType: Boolean = false
    ): ValueArgument {
        val fakeExpr = psiFactory.createSimpleName("tmpVar")

        context.trace.record(
            BindingContext.EXPRESSION_TYPE_INFO, fakeExpr, KotlinTypeInfo(
                type = type,
                dataFlowInfo = DataFlowInfo.EMPTY,
                jumpOutPossible = false,
                jumpFlowInfo = DataFlowInfo.EMPTY
            )
        )

        if (forceType) {
            context.trace.record(BindingContext.PROCESSED, fakeExpr, true)
        }

        return CallMaker.makeValueArgument(fakeExpr)
    }

    private fun resolveComposerEmit(
        implicitCtorTypes: List<KotlinType>,
        constructedType: KotlinType,
        hasBody: Boolean,
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        return resolveSubstitutableComposerMethod(
            KtxNameConventions.EMIT,
            listOfNotNull(
                builtIns.anyType,
                functionType(
                    parameterTypes = implicitCtorTypes,
                    returnType = constructedType
                ),
                functionType(),
                if (hasBody) functionType() else null
            ),
            constructedType,
            expressionToReportErrorsOn,
            context
        )
    }

    private fun resolveSingleValidationCall(
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType,
        validationType: ValidationType,
        checked: Boolean,
        attrType: KotlinType,
        lambdaArg: ValueArgument?,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve variable", expressionToReportErrorsOn
        )
        val contextToUse = context.replaceTraceAndCache(temporaryForVariable)
        val name = validationType.name.toLowerCase(Locale.ROOT).let {
            if (!checked) (it + "Unchecked") else it
        }
        val calleeExpression = psiFactory.createSimpleName(name)
        val call = makeCall(
            callElement = calleeExpression,
            calleeExpression = calleeExpression,
            valueArguments = listOfNotNull(
                makeValueArgument(attrType, contextToUse, forceType = true),
                lambdaArg
            ),
            receiver = TransientReceiver(receiverScope)
        )
        val results = callResolver.resolveCallWithGivenName(
            BasicCallResolutionContext.create(
                contextToUse,
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(contextToUse.dataFlowInfo, call)
            ),
            call,
            calleeExpression,
            Name.identifier(name)
        )

        if (results.isSuccess) return results.resultingCall

        if (results.resultCode == OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE) {

            // NOTE(lmr): We know the type of the attribute at this point, but it's possible for the validation call to require
            // some help in order to do the type inference for the call. We are just guessing here that the type is going to be
            // the attribute type, and not something more complicated. It is kind of a bummer that we need this and I wonder if
            // there isn't a cleaner way to do this.

            for (candidate in results.resultingCalls) {

                val typeParam = candidate.typeArguments.keys.singleOrNull() ?: continue

                if (!attrType.satisfiesConstraintsOf(typeParam)) continue

                val nextTempTrace = TemporaryTraceAndCache.create(
                    context, "trace to resolve variable", expressionToReportErrorsOn
                )

                val nextContext = context
                    .replaceTraceAndCache(nextTempTrace)
                    .replaceCollectAllCandidates(false)

                val substitutor = TypeSubstitutor.create(
                    mapOf(
                        typeParam.typeConstructor to attrType.asTypeProjection()
                    )
                )

                val nextCall = makeCall(
                    callElement = expressionToReportErrorsOn,
                    calleeExpression = calleeExpression,
                    receiver = TransientReceiver(composer.type),
                    valueArguments = candidate.candidateDescriptor.valueParameters.map {
                        makeValueArgument(it.type, nextContext)
                    }
                )

                val nextResults = callResolver.resolveCallWithKnownCandidate(
                    nextCall,
                    TracingStrategyImpl.create(calleeExpression, nextCall),
                    BasicCallResolutionContext.create(
                        nextContext,
                        nextCall,
                        CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                        DataFlowInfoForArgumentsImpl(nextContext.dataFlowInfo, nextCall)
                    ),
                    ResolutionCandidate.create(
                        nextCall,
                        candidate.candidateDescriptor,
                        candidate.dispatchReceiver,
                        candidate.explicitReceiverKind,
                        substitutor
                    ),
                    DataFlowInfoForArgumentsImpl(nextContext.dataFlowInfo, nextCall)
                )

                if (nextResults.isSuccess) {
                    nextTempTrace.commit()
                    return nextResults.resultingCall
                }
            }
        }

        return null
    }

    private fun resolveValidationCall(
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType,
        assignmentReceiverScope: KotlinType?,
        validationType: ValidationType,
        attrType: KotlinType,
        context: ExpressionTypingContext
    ): Triple<ResolvedCall<*>?, ResolvedCall<*>?, FunctionDescriptor?> {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve variable", expressionToReportErrorsOn
        )
        val contextToUse = context.replaceTraceAndCache(temporaryForVariable)

        val includeLambda = validationType != ValidationType.CHANGED

        // for call:
        // ValidatorType.set(AttrType, (AttrType) -> Unit): Boolean
        // ValidatorType.update(AttrType, (AttrType) -> Unit): Boolean
        // ValidatorType.changed(AttrType): Boolean

        // for emit:
        // ValidatorType.set(AttrType, ElementType.(AttrType) -> Unit): Unit
        // ValidatorType.update(AttrType, ElementType.(AttrType) -> Unit): Unit
        // ValidatorType.changed(AttrType): Unit

        val lambdaType = when {
            includeLambda -> functionType(
                parameterTypes = listOf(attrType),
                receiverType = assignmentReceiverScope
            )
            else -> null
        }
        val lambdaArg = lambdaType?.let { makeValueArgument(it, contextToUse) }
        val lambdaDescriptor = lambdaType?.let {
            createFunctionDescriptor(
                it,
                contextToUse
            )
        }

        val validationCall = resolveSingleValidationCall(
            expressionToReportErrorsOn = expressionToReportErrorsOn,
            receiverScope = receiverScope,
            validationType = validationType,
            checked = true,
            attrType = attrType,
            lambdaArg = lambdaArg,
            context = context
        )

        val uncheckedValidationCall = resolveSingleValidationCall(
            expressionToReportErrorsOn = expressionToReportErrorsOn,
            receiverScope = receiverScope,
            validationType = validationType,
            checked = false,
            attrType = attrType,
            lambdaArg = lambdaArg,
            context = context
        )

        return Triple(validationCall, uncheckedValidationCall, lambdaDescriptor)
    }

    private fun resolveSubstitutableComposerMethod(
        methodName: Name,
        argumentTypes: List<KotlinType>,
        typeToSubstitute: KotlinType?,
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve variable", expressionToReportErrorsOn
        )
        val contextToUse = context.replaceTraceAndCache(temporaryForVariable)

        val composerExpr = psiFactory.createSimpleName(methodName.asString())

        val call = makeCall(
            callElement = expressionToReportErrorsOn,
            calleeExpression = composerExpr,
            receiver = TransientReceiver(composer.type),
            valueArguments = argumentTypes.map { makeValueArgument(it, contextToUse) }
        )

        val results = callResolver.resolveCallWithGivenName(
            BasicCallResolutionContext.create(
                contextToUse,
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(contextToUse.dataFlowInfo, call)
            ),
            call,
            composerExpr,
            methodName
        )

        if (results.isSuccess) return results.resultingCall

        if (typeToSubstitute == null) return null

        val candidates = if (context.collectAllCandidates)
            results.allCandidates ?: emptyList()
        else results.resultingCalls

        for (candidate in candidates) {

            val typeParam = candidate.candidateDescriptor.typeParameters.singleOrNull() ?: continue

            if (!typeToSubstitute.satisfiesConstraintsOf(typeParam)) continue

            val nextTempTrace = TemporaryTraceAndCache.create(
                context, "trace to resolve variable", expressionToReportErrorsOn
            )

            val nextContext = context
                .replaceTraceAndCache(nextTempTrace)
                .replaceCollectAllCandidates(false)

            val substitutor = TypeSubstitutor.create(
                mapOf(
                    typeParam.typeConstructor to typeToSubstitute.asTypeProjection()
                )
            )

            val nextCall = makeCall(
                callElement = expressionToReportErrorsOn,
                calleeExpression = composerExpr,
                receiver = TransientReceiver(composer.type),
                valueArguments = candidate.candidateDescriptor.valueParameters.map {
                    makeValueArgument(it.type, nextContext)
                }
            )

            val nextResults = callResolver.resolveCallWithKnownCandidate(
                nextCall,
                TracingStrategyImpl.create(composerExpr, nextCall),
                BasicCallResolutionContext.create(
                    nextContext,
                    nextCall,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                    DataFlowInfoForArgumentsImpl(nextContext.dataFlowInfo, nextCall)
                ),
                ResolutionCandidate.create(
                    nextCall,
                    candidate.candidateDescriptor,
                    candidate.dispatchReceiver,
                    candidate.explicitReceiverKind,
                    substitutor
                ),
                DataFlowInfoForArgumentsImpl(nextContext.dataFlowInfo, nextCall)
            )

            if (nextResults.isSuccess) {
                nextTempTrace.commit()
                return nextResults.resultingCall
            }
        }

        return if (context.collectAllCandidates) null
        else resolveSubstitutableComposerMethod(
            methodName,
            argumentTypes,
            typeToSubstitute,
            expressionToReportErrorsOn,
            context.replaceCollectAllCandidates(true)
        )
    }

    private fun KtQualifiedExpression.elementChain(context: ExpressionTypingContext) {
        val moduleDescriptor = context.scope.ownerDescriptor.module
        val trace = context.trace
        val scopeForFirstPart = context.scope

        val path = asQualifierPartList()
        val firstPart = path.first()
        var currentDescriptor: DeclarationDescriptor? = scopeForFirstPart.findDescriptor(firstPart)
        currentDescriptor =
            currentDescriptor ?: moduleDescriptor.getPackage(FqName.topLevel(firstPart.name)).let {
                if (it.isEmpty()) null else it
            }

        if (currentDescriptor == null) return
        else storeSimpleNameExpression(firstPart.expression!!, currentDescriptor, trace)

        // TODO(lmr): we need to add visibility checks into this function...
        for (qualifierPartIndex in 1 until path.size) {
            val qualifierPart = path[qualifierPartIndex]

            val nextPackageOrClassDescriptor =
                when (currentDescriptor) {
                    // TODO(lmr): i wonder if we could allow this for Ktx. Seems like a nice to have
                    // TODO type aliases as qualifiers? (would break some assumptions in TypeResolver)
                    is TypeAliasDescriptor -> null
                    is ClassDescriptor -> {
                        var next: DeclarationDescriptor? = null
                        next = next
                            ?: currentDescriptor.unsubstitutedInnerClassesScope.findDescriptor(
                                qualifierPart
                            )
                        if (currentDescriptor.kind == ClassKind.OBJECT) {
                            next = next
                                ?: currentDescriptor.unsubstitutedMemberScope.findDescriptor(
                                    qualifierPart
                                )
                        }
                        val cod = currentDescriptor.companionObjectDescriptor
                        if (cod != null) {
                            next = next
                                ?: cod.unsubstitutedMemberScope.findDescriptor(qualifierPart)
                        }
                        next = next ?: currentDescriptor.staticScope.findDescriptor(qualifierPart)
                        next
                    }
                    is PackageViewDescriptor -> {
                        val packageView =
                            if (qualifierPart.typeArguments == null) {
                                moduleDescriptor.getPackage(
                                    currentDescriptor.fqName.child(qualifierPart.name)
                                )
                            } else null
                        if (packageView != null && !packageView.isEmpty()) {
                            packageView
                        } else {
                            currentDescriptor.memberScope.findDescriptor(qualifierPart)
                        }
                    }
                    is VariableDescriptor -> {
                        currentDescriptor.type.memberScope.findDescriptor(qualifierPart)
                    }
                    else -> null
                }

            if (nextPackageOrClassDescriptor == null) return
            else storeSimpleNameExpression(
                qualifierPart.expression!!,
                nextPackageOrClassDescriptor,
                trace
            )

            currentDescriptor = nextPackageOrClassDescriptor
        }
    }

    private fun storeSimpleNameExpression(
        expression: KtSimpleNameExpression,
        descriptor: DeclarationDescriptor,
        trace: BindingTrace
    ) {
        trace.record(BindingContext.REFERENCE_TARGET, expression, descriptor)
        UnderscoreUsageChecker.checkSimpleNameUsage(descriptor, expression, trace)

        val qualifier = when (descriptor) {
            is PackageViewDescriptor -> PackageQualifier(expression, descriptor)
            is ClassDescriptor -> ClassQualifier(expression, descriptor)
            is TypeParameterDescriptor -> TypeParameterQualifier(expression, descriptor)
            is TypeAliasDescriptor -> descriptor.classDescriptor?.let {
                TypeAliasQualifier(expression, descriptor, it)
            }
            else -> null
        }

        if (qualifier != null) {
            trace.record(BindingContext.QUALIFIER, qualifier.expression, qualifier)
        }
    }
}

// general utils
// ==============================

private val builtIns = DefaultBuiltIns.Instance

private fun functionType(
    parameterTypes: List<KotlinType> = emptyList(),
    annotations: Annotations = Annotations.EMPTY,
    returnType: KotlinType = builtIns.unitType,
    receiverType: KotlinType? = null
): KotlinType = createFunctionType(
    builtIns = builtIns,
    annotations = annotations,
    parameterNames = null,
    parameterTypes = parameterTypes,
    receiverType = receiverType,
    returnType = returnType
)

private fun KotlinType.satisfiesConstraintsOf(T: TypeParameterDescriptor): Boolean {
    return T.upperBounds.all { isSubtypeOf(it) }
}

// We want to return null in cases where types mismatch, so we use this heuristic to find out. I think there might be a more robust
// way to find this out, but I'm not sure what it would be
private fun BindingTrace.hasTypeMismatchErrorsOn(element: KtElement): Boolean =
    bindingContext.diagnostics.forElement(element).any { it.severity == Severity.ERROR }

private fun KtExpression.asQualifierPartList(): List<QualifiedExpressionResolver.QualifierPart> {
    val result = SmartList<QualifiedExpressionResolver.QualifierPart>()

    fun addQualifierPart(expression: KtExpression?): Boolean {
        if (expression is KtSimpleNameExpression) {
            result.add(
                QualifiedExpressionResolver.ExpressionQualifierPart(
                    expression.getReferencedNameAsName(),
                    expression
                )
            )
            return true
        }
        return false
    }

    var expression: KtExpression? = this
    while (true) {
        if (addQualifierPart(expression)) break
        if (expression !is KtQualifiedExpression) break

        addQualifierPart(expression.selectorExpression)

        expression = expression.receiverExpression
    }

    return result.asReversed()
}

private fun HierarchicalScope.findDescriptor(
    part: QualifiedExpressionResolver.QualifierPart
): DeclarationDescriptor? {
    return findFirstFromMeAndParent {
        it.findVariable(part.name, part.location)
            ?: it.findFunction(part.name, part.location)
            ?: it.findClassifier(part.name, part.location)
    }
}

private fun MemberScope.findDescriptor(
    part: QualifiedExpressionResolver.QualifierPart
): DeclarationDescriptor? {
    return this.getContributedClassifier(part.name, part.location)
        ?: getContributedFunctions(part.name, part.location).singleOrNull()
        ?: getContributedVariables(part.name, part.location).singleOrNull()
}

private fun AttributeInfo.toValueArgument(name: String, named: Boolean): ValueArgument {
    val argumentName = if (named) object : ValueArgumentName {
        override val asName: Name
            get() = Name.identifier(name)
        override val referenceExpression: KtSimpleNameExpression?
            get() = key
    } else null
    return object : ValueArgument {
        override fun getArgumentExpression() = value
        override fun getArgumentName() = argumentName
        override fun isNamed() = named
        override fun asElement(): KtElement = value
        override fun getSpreadElement(): LeafPsiElement? = null
        override fun isExternal() = true
    }
}

/**
 * This function was copied verbatim from descriptorUtils.kt from ide-common.  For some reason, importing this method into our module
 * would not work and would cause a NoClassDefFound exception.
 */
private fun descriptorsEqualWithSubstitution(
    descriptor1: DeclarationDescriptor?,
    descriptor2: DeclarationDescriptor?
): Boolean {
    if (descriptor1 == descriptor2) return true
    if (descriptor1 == null || descriptor2 == null) return false
    if (descriptor1 !is CallableDescriptor) return true
    descriptor2 as CallableDescriptor

    val typeChecker = KotlinTypeCheckerImpl.withAxioms(
        object : KotlinTypeChecker.TypeConstructorEquality {
            override fun equals(a: TypeConstructor, b: TypeConstructor): Boolean {
                val typeParam1 = a.declarationDescriptor as? TypeParameterDescriptor
                val typeParam2 = b.declarationDescriptor as? TypeParameterDescriptor
                if (typeParam1 != null && typeParam2 != null &&
                    typeParam1.containingDeclaration == descriptor1 &&
                    typeParam2.containingDeclaration == descriptor2
                ) {
                    return typeParam1.index == typeParam2.index
                }

                return a == b
            }
        }
    )

    if (!typeChecker.equalTypesOrNulls(descriptor1.returnType, descriptor2.returnType)) return false

    val parameters1 = descriptor1.valueParameters
    val parameters2 = descriptor2.valueParameters
    if (parameters1.size != parameters2.size) return false
    for ((param1, param2) in parameters1.zip(parameters2)) {
        if (!typeChecker.equalTypes(param1.type, param2.type)) return false
    }
    // NOTE(lmr): edit
    // this check was added
    if (descriptor1.javaClass !== descriptor2.javaClass) return false
    // NOTE(lmr): /end
    return true
}

private val ResolvedCall<*>.semanticCall: ResolvedCall<*>
    get() = when (this) {
        is VariableAsFunctionResolvedCall -> variableCall
        else -> this
    }

private val Collection<ValueParameterDescriptor>.possibleChildrenParameter:
        ValueParameterDescriptor?
    get() = maxBy { it.index }?.let { if (it.type.isFunctionType) it else null }

// move these to naming conventions???
const val CHILDREN_KEY = "<children>"
const val TAG_KEY = "<tag>"

fun makeCall(
    callElement: KtElement,
    calleeExpression: KtExpression? = null,
    valueArguments: List<ValueArgument> = emptyList(),
    receiver: Receiver? = null,
    dispatchReceiver: ReceiverValue? = null
): Call {
    return object : Call {
        override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver
        override fun getValueArgumentList(): KtValueArgumentList? = null
        override fun getTypeArgumentList(): KtTypeArgumentList? = null
        override fun getExplicitReceiver(): Receiver? = receiver
        override fun getCalleeExpression(): KtExpression? = calleeExpression
        override fun getValueArguments(): List<ValueArgument> = valueArguments
        override fun getCallElement(): KtElement = callElement
        override fun getFunctionLiteralArguments(): List<LambdaArgument> = emptyList()
        override fun getTypeArguments(): List<KtTypeProjection> = emptyList()
        override fun getCallType(): Call.CallType = Call.CallType.DEFAULT
        override fun getCallOperationNode(): ASTNode? = null
    }
}

fun createFunctionDescriptor(
    type: KotlinType,
    context: ExpressionTypingContext
): FunctionDescriptor {
    return AnonymousFunctionDescriptor(
        context.scope.ownerDescriptor,
        Annotations.EMPTY,
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        SourceElement.NO_SOURCE,
        false
    ).apply {
        initialize(
            type.getReceiverTypeFromFunctionType()?.let {
                DescriptorFactory.createExtensionReceiverParameterForCallable(
                    this,
                    it,
                    Annotations.EMPTY)
            },
            null,
            emptyList(),
            type.getValueParameterTypesFromFunctionType().mapIndexed { i, t ->
                ValueParameterDescriptorImpl(
                    containingDeclaration = this,
                    original = null,
                    index = i,
                    annotations = Annotations.EMPTY,
                    name = t.type.extractParameterNameFromFunctionTypeArgument()
                        ?: Name.identifier("p$i"),
                    outType = t.type,
                    declaresDefaultValue = false,
                    isCrossinline = false,
                    isNoinline = false,
                    varargElementType = null,
                    source = SourceElement.NO_SOURCE
                )
            },
            type.getReturnTypeFromFunctionType(),
            Modality.FINAL,
            Visibilities.LOCAL,
            null
        )
        isOperator = false
        isInfix = false
        isExternal = false
        isInline = false
        isTailrec = false
        isSuspend = false
        isExpect = false
        isActual = false
    }
}

fun KotlinType.satisfiesConstraintsOf(bounds: List<KotlinType>): Boolean {
    return bounds.all { isSubtypeOf(it) }
}

fun KotlinType.upperBounds(): List<KotlinType> {
    return if (isTypeParameter()) {
        TypeUtils.getTypeParameterDescriptorOrNull(this)?.upperBounds ?: emptyList()
    } else {
        listOf(this)
    }
}

// util classes
// ========================
class ImplicitCtorValueArgument(val type: KotlinType) : ValueArgument {
    override fun getArgumentExpression(): KtExpression? = null
    override fun getArgumentName(): ValueArgumentName? = null
    override fun isNamed(): Boolean = false
    override fun asElement(): KtElement = error("tried to get element")
    override fun getSpreadElement(): LeafPsiElement? = null
    override fun isExternal(): Boolean = true
}

class AttributeInfo(
    val value: KtExpression,
    val key: KtSimpleNameExpression?,
    val name: String
)

class NoInterceptionCallResolver(private val callResolver: CallResolver) {
    fun resolveCallWithGivenName(
        context: ResolutionContext<*>,
        call: Call,
        functionReference: KtReferenceExpression,
        name: Name
    ): OverloadResolutionResults<FunctionDescriptor> {
        context.trace.record(
            ComposeWritableSlices.IGNORE_COMPOSABLE_INTERCEPTION,
            call,
            true
        )
        return callResolver.resolveCallWithGivenName(
            context,
            call,
            functionReference,
            name
        )
    }

    fun resolveCallWithKnownCandidate(
        call: Call,
        tracing: TracingStrategy,
        context: ResolutionContext<*>,
        candidate: ResolutionCandidate<FunctionDescriptor>,
        dataFlowInfoForArguments: MutableDataFlowInfoForArguments?
    ): OverloadResolutionResults<FunctionDescriptor> {
        context.trace.record(
            ComposeWritableSlices.IGNORE_COMPOSABLE_INTERCEPTION,
            call,
            true
        )
        return callResolver.resolveCallWithKnownCandidate(
            call,
            tracing,
            context,
            candidate,
            dataFlowInfoForArguments
        )
    }

    fun resolveSimpleProperty(
        context: BasicCallResolutionContext
    ): OverloadResolutionResults<VariableDescriptor> {
        return callResolver.resolveSimpleProperty(
            context
        )
    }

    fun resolveFunctionCall(
        context: BasicCallResolutionContext
    ): OverloadResolutionResults<FunctionDescriptor> {
        context.trace.record(
            ComposeWritableSlices.IGNORE_COMPOSABLE_INTERCEPTION,
            context.call,
            true
        )
        return callResolver.resolveFunctionCall(
            context
        )
    }

    fun <T : CallableDescriptor> computeTasksAndResolveCall(
        context: BasicCallResolutionContext,
        name: Name,
        referenceExpression: KtReferenceExpression,
        kind: NewResolutionOldInference.ResolutionKind
    ): OverloadResolutionResults<T> {
        context.trace.record(
            ComposeWritableSlices.IGNORE_COMPOSABLE_INTERCEPTION,
            context.call,
            true
        )
        return callResolver.computeTasksAndResolveCall(
            context,
            name,
            referenceExpression,
            kind
        )
    }
}