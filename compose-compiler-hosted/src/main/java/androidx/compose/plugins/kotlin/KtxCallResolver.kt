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
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.FieldDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.LambdaArgument
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.ValueArgumentName
import androidx.compose.plugins.kotlin.analysis.ComposeDefaultErrorMessages
import androidx.compose.plugins.kotlin.analysis.ComposeErrors
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.ObservableBindingTrace
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.TraceEntryFilter
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.checkers.UnderscoreUsageChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatchStatus
import org.jetbrains.kotlin.resolve.calls.model.ArgumentUnmapped
import org.jetbrains.kotlin.resolve.calls.model.DataFlowInfoForArgumentsImpl
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyForInvoke
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyImpl
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
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
import org.jetbrains.kotlin.types.expressions.ExpressionTypingFacade
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.equalTypesOrNulls
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.isNothingOrNullableNothing
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This class is used to resolve a KTX Element to the corresponding set of calls on the composer, and the individual calls for
 * each attribute, etc.
 */
class KtxCallResolver(
    private val callResolver: CallResolver,
    private val facade: ExpressionTypingFacade,
    private val project: Project,
    private val composableAnnotationChecker: ComposableAnnotationChecker
) {
    companion object {
        val resolving: ThreadLocal<AtomicBoolean> = object : ThreadLocal<AtomicBoolean>() {
            override fun initialValue() = AtomicBoolean()
        }
    }

    private class TempResolveInfo(
        val valid: Boolean,
        val trace: TemporaryTraceAndCache,
        val unusedAttributes: Set<String>,
        val usedAttributes: Set<String>,
        val missingRequiredAttributes: List<DeclarationDescriptor>,
        val build: () -> EmitOrCallNode
    ) {
        val attributesLeft: Int
            get() = unusedAttributes.count { it != TAG_KEY }
    }

    private class TempParameterInfo(
        val attribute: AttributeInfo,
        val descriptor: DeclarationDescriptor,
        val type: KotlinType
    )

    // NOTE(lmr): I am unsure of the performance consequences of using KtPsiFactory. it appears to create a file for every
    // call, which seems like something we definitely do not want, but it's also used in resolving for(..) loops, so
    // maybe it's not terrible.
    private val psiFactory = KtPsiFactory(project, markGenerated = false)

    // The type of the `composer` variable in scope of the KTX tag
    private var composerType: KotlinType = builtIns.unitType
    // A ResolvedCall to "get" the composer variable in scope of the KTX element.
    private var composerResolvedCall: ResolvedCall<*>? = null
    // A ResolvedCall to the `joinKey(Any, Any?)` method on the composer in scope.
    private var joinKeyCall: ResolvedCall<*>? = null

    private var infixOrCall: ResolvedCall<*>? = null

    private lateinit var mainElement: KtElement

    private lateinit var tagExpressions: List<KtExpression>

    private lateinit var module: ModuleDescriptor

    // Set of valid upper bound types that were defined on the composer that can't have children
    // For android, this should be [View]
    private val emitSimpleUpperBoundTypes = mutableSetOf<KotlinType>()

    private fun KotlinType.isEmittable() =
        !isError && !isNothingOrNullableNothing() && emitSimpleUpperBoundTypes.any {
            isSubtypeOf(it)
        }

    // Set of valid upper bound types that were defined on the composer that can have children.
    // For android, this would be [ViewGroup]
    private val emitCompoundUpperBoundTypes = mutableSetOf<KotlinType>()

    private fun KotlinType.isCompoundEmittable() = !isError &&
            !this.isNothingOrNullableNothing() &&
            emitCompoundUpperBoundTypes.any {
                isSubtypeOf(it)
            }

    // The specification for `emit` on a composer allows for the `ctor` parameter to be a function type
    // with any number of parameters. We allow for these parameters to be used as parameters in the
    // Constructors that are emitted with a KTX tag. These parameters can be overridden with attributes
    // in the KTX tag, but if there are required parameters with a type that matches one declared in the
    // ctor parameter, we will resolve it automatically with the value passed in the `ctor` lambda.
    //
    // In order to do this resolution, we store a list of pairs of "upper bounds" to parameter types. For example,
    // the following emit call:
    //
    //      fun <T : View> emit(key: Any, ctor: (context: Context) -> T, update: U<T>.() -> Unit)
    //
    // would produce a Pair of [View] to [Context]
    private val emittableTypeToImplicitCtorTypes =
        mutableListOf<Pair<List<KotlinType>, Set<KotlinType>>>()

    private fun isImplicitConstructorParam(
        param: ValueParameterDescriptor,
        fn: CallableDescriptor
    ): Boolean {
        val returnType = fn.returnType ?: return false
        val paramType = param.type
        for ((upperBounds, implicitTypes) in emittableTypeToImplicitCtorTypes) {
            if (!implicitTypes.any { it.isSubtypeOf(paramType) }) continue
            if (!returnType.satisfiesConstraintsOf(upperBounds)) continue
            return true
        }
        return false
    }

    fun initializeFromKtxElement(element: KtxElement, context: ExpressionTypingContext): Boolean {

        val wasResolving = resolving.get().getAndSet(true)
        module = context.scope.ownerDescriptor.module

        // we want to report errors on the tag names (open and closing), and not the entire element, since
        // that would be too noisy
        tagExpressions = listOfNotNull(
            element.simpleTagName,
            element.simpleClosingTagName,
            element.qualifiedTagName,
            element.qualifiedClosingTagName
        )

        val result = resolveComposer(element, context)
        if (wasResolving) resolving.get().set(false)
        return result
    }

    fun initializeFromCall(call: Call, context: ExpressionTypingContext): Boolean {
        val wasResolving = resolving.get().getAndSet(true)

        val callee = call.calleeExpression ?: error("Expected calleeExpression")

        module = context.scope.ownerDescriptor.module

        tagExpressions = listOf(callee)

        val result = resolveComposer(callee, context)

        if (wasResolving) resolving.get().set(false)

        return result
    }

    /**
     * KTX tags are defined to resolve a "composer" in the scope of the tag itself, and then the tag translates into a call
     * or a set of calls on that composer instance. This method should be called first, and will resolve the composer in scope
     * and record various pieces of metadata about the composer that will make resolving the tag possible. If it returns false
     * then something went wrong and you should not try and resolve the tag. If the method returns false, at least one
     * diagnostic will have been added to the tag somewhere to indicate that there was a problem.
     */
    private fun resolveComposer(element: KtExpression, context: ExpressionTypingContext): Boolean {

        // The composer is currently resolved as whatever is currently in scope with the name "composer".
        val resolvedComposer = resolveVar(KtxNameConventions.COMPOSER, element, context)

        if (!resolvedComposer.isSuccess) {
            ComposeErrors.NO_COMPOSER_FOUND.report(context, tagExpressions)
            return false
        }

        val composerResolvedCall = resolvedComposer.resultingCall.also { composerResolvedCall = it }

        val descriptor = composerResolvedCall.resultingDescriptor

        composerType = when (descriptor) {
            is PropertyDescriptor -> descriptor.type
            is VariableDescriptor -> descriptor.type
            // if composer isn't a property or variable, we don't currently know how to resolve it...
            else -> {
                ComposeErrors.NO_COMPOSER_FOUND.report(context, tagExpressions)
                return false
            }
        }

        val emitCandidates = resolveComposerMethodCandidates(
            element,
            KtxNameConventions.EMIT,
            context
        )

        for (candidate in emitCandidates.map { it.candidateDescriptor }) {
            if (candidate.name != KtxNameConventions.EMIT) continue
            if (candidate !is SimpleFunctionDescriptor) continue
            val params = candidate.valueParameters
            // NOTE(lmr): we could report diagnostics on some of these? it seems strange to emit diagnostics about a function
            // that is not necessarily being used though. I think it's probably better to just ignore them here.

            // the signature of emit that we are looking for has 3 or 4 parameters
            if (params.size < 3 || params.size > 4) continue
            val ctorParam = params.find { it.name == KtxNameConventions.EMIT_CTOR_PARAMETER }
                ?: continue
            if (!ctorParam.type.isFunctionTypeOrSubtype) continue

            // the return type from the ctor param is the "upper bound" of the node type. It will often be a generic type with constraints.
            val upperBounds = ctorParam.type.getReturnTypeFromFunctionType().upperBounds()

            // the ctor param can have parameters itself, which we interpret as implicit parameter types that the composer knows how to
            // automatically provide to the component. In the case of Android Views, this is how we automatically provide Context.
            val implicitParamTypes = ctorParam.type.getValueParameterTypesFromFunctionType().map {
                it.type
            }

            for (implicitType in implicitParamTypes) {
                emittableTypeToImplicitCtorTypes.add(upperBounds to implicitParamTypes.toSet())
            }

            emitSimpleUpperBoundTypes.addAll(upperBounds)

            if (params.any { it.name == KtxNameConventions.EMIT_CHILDREN_PARAMETER }) {
                emitCompoundUpperBoundTypes.addAll(upperBounds)
            }
        }

        if (emitSimpleUpperBoundTypes.isEmpty()) {

            // if the composer has no valid `emit` candidates, but *does* valid `call` methods, we will report no errors.
            // It's strange, but it's possible for a composer that only handles `call` to be useful. To be sure, we first
            // look up all of the call candidates.
            val callCandidates = resolveComposerMethodCandidates(
                element,
                KtxNameConventions.EMIT,
                context
            )

            if (callCandidates.isEmpty()) {
                ComposeErrors.INVALID_COMPOSER_IMPLEMENTATION.report(
                    context,
                    tagExpressions,
                    composerType,
                    "Couldn't find any valid `call(...)` or `emit(...)` methods"
                )
            }
        }

        val left = psiFactory.createSimpleName("a")
        val right = psiFactory.createSimpleName("b")

        context.trace.record(
            BindingContext.EXPRESSION_TYPE_INFO, left, KotlinTypeInfo(
                type = builtIns.booleanType,
                dataFlowInfo = DataFlowInfo.EMPTY,
                jumpOutPossible = false,
                jumpFlowInfo = DataFlowInfo.EMPTY
            )
        )

        context.trace.record(
            BindingContext.EXPRESSION_TYPE_INFO, right, KotlinTypeInfo(
                type = builtIns.booleanType,
                dataFlowInfo = DataFlowInfo.EMPTY,
                jumpOutPossible = false,
                jumpFlowInfo = DataFlowInfo.EMPTY
            )
        )

        infixOrCall = resolveInfixOr(context)

        joinKeyCall = resolveJoinKey(
            expressionToReportErrorsOn = tagExpressions.firstOrNull() ?: return false,
            context = context
        ) ?: run {
            ComposeErrors.INVALID_COMPOSER_IMPLEMENTATION.report(
                context,
                tagExpressions,
                composerType,
                "Couldn't find valid method 'fun joinKey(Any, Any?): Any'"
            )
            return false
        }

        return true
    }

    fun resolveFromKtxElement(
        element: KtxElement,
        context: ExpressionTypingContext
    ): ResolvedKtxElementCall {
        val wasResolving = resolving.get().getAndSet(true)
        mainElement = element

        val openTagExpr = element.simpleTagName ?: element.qualifiedTagName
            ?: error("shouldn't happen")
        val closeTagExpr = element.simpleClosingTagName ?: element.qualifiedClosingTagName
        val attributes = element.attributes

        val tmpTraceAndCache = TemporaryTraceAndCache.create(
            context,
            "trace for ktx tag",
            element
        )

        val contextToUse = context.replaceTraceAndCache(tmpTraceAndCache)

        val attrInfos = mutableMapOf<String, AttributeInfo>()

        for (attr in attributes) {
            var key = attr.key
            var value = attr.value
            val name = key.getReferencedName()
            var isPunned = false
            if (attrInfos.contains(name)) {
                contextToUse.trace.reportFromPlugin(
                    ComposeErrors.DUPLICATE_ATTRIBUTE.on(key),
                    ComposeDefaultErrorMessages
                )
            }
            if (value == null && attr.equals != null) {
                // this isn't punning because there is an equal sign, but there is not yet a value expression. this happens often
                // when typing and with code completion. To deal with it, we just pass in a fake psi node as the value. This will
                // result in some diagnostics, but it's okay. We want to continue to pass the attribute in as it can affect which
                // target descriptor gets chosen.
                value = psiFactory.createSimpleName("_")
            }
            if (value == null && attr.equals == null) {
                // punning...
                // punning has a single expression that both acts as reference to the value and to the property/setter. As a result, we
                // need to save the descriptors that it targets into a different writable slice that we can surface later in a
                // reference so that users cam Cmd-Click to either target. Additionally, for the purposes of codegen it's more important
                // that the expression gets resolved to its *value*, and not its *target attribute*. In order to ensure this, we end up
                // running `getTypeInfo` on any attributes that are punned before doing any other resolution.
                val temporaryForPunning = TemporaryTraceAndCache.create(
                    contextToUse, "trace to resolve reference for punning", key
                )

                facade.getTypeInfo(
                    key,
                    contextToUse.replaceTraceAndCache(temporaryForPunning)
                )

                temporaryForPunning.trace[BindingContext.REFERENCE_TARGET, key]?.let {
                    // save the reference into a Set that we can use later in a custom reference
                    temporaryForPunning.trace.recordAttributeKeyRef(key, it)
                }

                // in the case of a punned attribute, we create a fake psi node for the "key", or else some type resolution stuff will
                // get messed up (for example, smart casting).
                value = key
                key = psiFactory.createSimpleName(name)
                isPunned = true

                temporaryForPunning.commit()
            }

            attrInfos[name] = AttributeInfo(
                value = value ?: error("expected a value expression"),
                key = key,
                name = name,
                isPunned = isPunned
            )
        }

        // The tag expression and the body expression are both implicitly types of "attributes" for the tag, but they
        // aren't explicitly names. As a result, we put them into the `attrInfos` map with special keys
        element.bodyLambdaExpression?.let {
            attrInfos[CHILDREN_KEY] =
                AttributeInfo(
                    value = it,
                    key = null,
                    name = CHILDREN_KEY,
                    isPunned = false
                )
        }

        // we want to resolve all reference targets on the open tag on the closing tag as well, but we don't want
        // to have to execute the resolution code for both the open and close each time, so we create a binding
        // trace that will observe for traces on the open tag and copy them over to the closing tag if one
        // exists. We choose to only use this trace when we know that useful slices might show up on the tag.
        var traceForOpenClose: BindingTrace = tmpTraceAndCache.trace
        closeTagExpr?.let {
            traceForOpenClose = referenceCopyingTrace(
                openTagExpr,
                closeTagExpr,
                tmpTraceAndCache.trace
            )
        }
        val receiver = resolveReceiver(
            openTagExpr,
            contextToUse.replaceBindingTrace(traceForOpenClose)
        )

        attrInfos[TAG_KEY] =
            AttributeInfo(
                value = when (receiver) {
                    is ExpressionReceiver -> receiver.expression
                    else -> openTagExpr
                },
                key = null,
                name = TAG_KEY,
                isPunned = false
            )

        val result = resolve(
            openTagExpr,
            closeTagExpr,
            receiver,
            attrInfos,
            contextToUse
        )

        // Instead of committing the whole temp trace, we do so individually because there are some diagnostics that we are going
        // to switch out with others.
        tmpTraceAndCache.cache.commit()

        // if the tag target is non-namespaced and there is a closing tag as well, the default "Import" quick fix will not
        // work appropriately. To counteract this, we intercept UNRESOLVED_REFERENCE on the tag target specifically, and replace
        // it with an UNRESOLVED_TAG diagnostic. We have our own import quickfix that knows how to handle this properly.
        // Ideally in the long run we can fix this in a different / better way.
        val isSimpleTag = openTagExpr is KtSimpleNameExpression && closeTagExpr != null

        val diagnostics = tmpTraceAndCache.trace.bindingContext.diagnostics
        for (diagnostic in diagnostics) {
            if (isSimpleTag && diagnostic.psiElement in tagExpressions &&
                diagnostic.factory == Errors.UNRESOLVED_REFERENCE) {
                val refExpression = diagnostic.psiElement as KtReferenceExpression
                // here we want to swallow this diagnostic and replace it with UNRESOLVED_TAG, so that the quickfix works properly:
                context.trace.reportFromPlugin(
                    ComposeErrors.UNRESOLVED_TAG.on(refExpression, refExpression),
                    ComposeDefaultErrorMessages
                )
            } else {
                context.trace.reportDiagnosticOnce(diagnostic)
            }
        }

        // commit, but don't include diagnostics
        tmpTraceAndCache.trace.commit({ _, _ -> true }, false)

        if (wasResolving) resolving.get().set(false)
        return result
    }

    fun resolveFromCall(
        call: Call,
        context: ExpressionTypingContext
    ): ResolvedKtxElementCall {
        val wasResolving = resolving.get().getAndSet(true)
        val callee = call.calleeExpression ?: error("Expected calleeExpression")

        mainElement = callee

        val openTagExpr = callee
        val closeTagExpr = null

        val tmpTraceAndCache = TemporaryTraceAndCache.create(
            context,
            "trace for ktx tag",
            callee
        )

        val contextToUse = context.replaceTraceAndCache(tmpTraceAndCache)

        val attrInfos = mutableMapOf<String, AttributeInfo>()

        for (arg in call.valueArguments) {
            if (arg is KtLambdaArgument) continue
            val argName = arg.getArgumentName()

            if (argName == null) TODO("indexed arguments not yet supported!")

            val key = argName.referenceExpression
            val value = arg.getArgumentExpression()
            val name = argName.asName.asString()

            // NOTE: We don't have to check for duplicate argument names, that will be done elsewhere.

            // NOTE: We don't have to deal with punning. punning isn't supported in FCS.

            attrInfos[name] = AttributeInfo(
                value = value ?: error("expected a value expression"),
                key = key,
                name = name,
                isPunned = false
            )
        }

        for (arg in call.functionLiteralArguments) {
            if (attrInfos.containsKey(CHILDREN_KEY))
                error("Only one children argument supported at a time")
            attrInfos[CHILDREN_KEY] =
                AttributeInfo(
                    value = arg.getLambdaExpression() ?: error("expected a value expression"),
                    key = null,
                    name = CHILDREN_KEY,
                    isPunned = false
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
                name = TAG_KEY,
                isPunned = false
            )

        val result = resolve(
            openTagExpr,
            closeTagExpr,
            receiver,
            attrInfos,
            contextToUse
        )

        tmpTraceAndCache.commit()
        if (wasResolving) resolving.get().set(false)
        return result
    }

    /**
     * This call is the main function of this class, and will take in a KtxElement and return an object with all of the information
     * necessary to generate the code for the KTX tag. This method will always return a result, but the result may contain errors
     * and it is the responsibility of the consumer of this class to handle that properly.
     */
    private fun resolve(
        openTagExpr: KtExpression,
        closeTagExpr: KtExpression?,
        receiver: Receiver?,
        attrInfos: Map<String, AttributeInfo>,
        context: ExpressionTypingContext
    ): ResolvedKtxElementCall {

        val usedAttributes = mutableSetOf<String>()

        val missingRequiredAttributes = mutableListOf<DeclarationDescriptor>()

        val emitOrCall = resolveChild(
            openTagExpr,
            ResolveStep.Root(openTagExpr, closeTagExpr),
            makeCall(
                openTagExpr,
                receiver = receiver,
                calleeExpression = when (openTagExpr) {
                    is KtQualifiedExpression -> openTagExpr.selectorExpression
                    is KtSimpleNameExpression -> openTagExpr
                    else -> null
                }
            ),
            attrInfos,
            usedAttributes,
            missingRequiredAttributes,
            context,
            recurseOnUnresolved = false
        )

        // TODO(lmr): validate that if it bottoms out at an emit(...) that it doesn't have any call(...)s

        val errorNode = emitOrCall.errorNode()

        errorNode?.let { error ->
            when (error) {
                is ErrorNode.NonCallableRoot -> {
                    val type = facade.getTypeInfo(
                        openTagExpr,
                        context.withThrowawayTrace(openTagExpr)
                    ).type

                    if (type != null) {
                        ComposeErrors.INVALID_TAG_TYPE.report(
                            context,
                            tagExpressions,
                            type,
                            emitSimpleUpperBoundTypes
                        )
                    } else {
                        ComposeErrors.INVALID_TAG_DESCRIPTOR.report(
                            context,
                            tagExpressions,
                            emitSimpleUpperBoundTypes
                        )
                    }
                }
                is ErrorNode.NonEmittableNonCallable -> {
                    // TODO(lmr): diagnostic
                    // TODO(lmr): we should probably put more info here, saying the composerType and stuff like that
                    // "ktx tag terminated with type "Foo", which is neither an emittable, nor callable
                    ComposeErrors.INVALID_TAG_TYPE.report(
                        context,
                        tagExpressions,
                        error.type,
                        emitSimpleUpperBoundTypes
                    )
                }
                is ErrorNode.RecursionLimitAmbiguousAttributesError -> {
                    ComposeErrors.AMBIGUOUS_ATTRIBUTES_DETECTED.report(
                        context,
                        tagExpressions,
                        error.attributes
                    )
                }
                is ErrorNode.RecursionLimitError -> {
                    ComposeErrors.CALLABLE_RECURSION_DETECTED.report(context, tagExpressions)
                }
            }
        }

        val constantChecker = ConstantExpressionEvaluator(
            project = project,
            module = context.scope.ownerDescriptor.module,
            languageVersionSettings = context.languageVersionSettings
        )

        val attributeNodes = emitOrCall
            .allAttributes()
            .mapNotNull { it as? AttributeNode }
            .groupBy { it.name }

        // we want to return a list of the used "AttributeNodes" to the top level call object that we return,
        // so we dig through all of the attributes of the AST and return a unique list. For efficiency, whether
        // or not an AttributeNode is "static" or not is not determined until now, since we don't want to have to
        // calculate it multiple times for the same attribute, so as we loop through this list, we calculate
        // whether it is static just once and then update all of the others as we go through them.
        val usedAttributeNodes = attributeNodes
            .mapValues { it.value.first() }
            .values
            .map { node ->
                val static = isStatic(
                    node.expression,
                    context,
                    node.type,
                    constantChecker
                )

                // update all of the nodes in the AST as "static"
                attributeNodes[node.name]?.forEach { it.isStatic = static }

                // return a node for the root of the AST that codegen can use
                AttributeNode(
                    name = node.name,
                    descriptor = node.descriptor,
                    expression = node.expression,
                    type = node.type,
                    isStatic = static
                )
            }

        // it's okay if the tag doesn't show up as used, so we remove it from this list
        val unusedAttributes = (attrInfos - usedAttributes - TAG_KEY).toMutableMap()

        if (unusedAttributes.isNotEmpty()) {

            // if we have some unused attributes, we want to provide some helpful diagnostics on them, so we grab
            // every possible attribute for the call. Note that we only want to run this (expensive) calculation in
            // cases where there *were* unused attributes, so the clean compile path should avoid this.
            val allPossibleAttributes = emitOrCall.allPossibleAttributes()

            loop@ for (attr in unusedAttributes.values) {
                when (attr.name) {
                    CHILDREN_KEY -> {
                        if (emitOrCall is EmitCallNode) {
                            val type = emitOrCall.memoize.ctorCall?.resultingDescriptor?.returnType
                                ?: error("expected a return type")
                            if (!type.isCompoundEmittable()) {
                                context.trace.reportFromPlugin(
                                    ComposeErrors.CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED.on(
                                        openTagExpr
                                    ),
                                    ComposeDefaultErrorMessages
                                )
                            } else {
                                // this is a compound emittable, so we will interpret the children block as just code to execute
                                unusedAttributes.remove(CHILDREN_KEY)
                            }
                        } else {
                            facade.getTypeInfo(
                                attr.value,
                                context.replaceExpectedType(functionType().makeComposable(module))
                            )
                            val possibleChildren = allPossibleAttributes[CHILDREN_KEY]
                                ?: emptyList()
                            if (possibleChildren.isNotEmpty()) {
                                context.trace.reportFromPlugin(
                                    ComposeErrors.UNRESOLVED_CHILDREN.on(
                                        openTagExpr,
                                        possibleChildren.map { it.type }
                                    ),
                                    ComposeDefaultErrorMessages
                                )
                            } else {
                                context.trace.reportFromPlugin(
                                    ComposeErrors.CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED.on(
                                        openTagExpr
                                    ),
                                    ComposeDefaultErrorMessages
                                )
                            }
                        }
                    }
                    else -> {
                        val exprToReportOn = if (attr.isPunned) attr.value else (attr.key
                            ?: error("expected non-null key expression"))
                        val valueType = facade.getTypeInfo(attr.value, context).type
                        // if the value expression is an unresolved reference, we don't need to put a diagnostic on the key
                        val valueIsUnresolvedRef = attr.value is KtReferenceExpression &&
                                context.trace[BindingContext.REFERENCE_TARGET, attr.value] != null

                        val descriptors = emitOrCall.resolvedCalls().flatMap {
                            listOfNotNull(
                                it.resultingDescriptor,
                                it.resultingDescriptor.returnType?.let { t ->
                                    if (t.isUnit()) null
                                    else t.constructor.declarationDescriptor
                                }
                            )
                        }

                        // since extension functions won't show up if we just traverse the member scope of the types in our call,
                        // we might be giving inaccurate diagnostics around what types are accepted for this attribute. Since we have
                        // the name, we can search for all possible candidates of attributes on a given node. This is expensive, but
                        // since we are only in the erroneous path, it shouldn't be a big deal.
                        val attrsOfSameKey = resolveAttributeCandidatesGivenNameAndNode(
                            emitOrCall,
                            attr.name,
                            context.withThrowawayTrace(openTagExpr)
                        )

                        if (attrsOfSameKey.isNotEmpty()) {
                            // NOTE(lmr): it would be great if we could record multiple possible types here instead of just one for
                            // autocomplete
                            context.trace.record(
                                BindingContext.EXPECTED_EXPRESSION_TYPE,
                                attr.value,
                                attrsOfSameKey.first().type
                            )

                            // even if the type doesn't match the attribute, we should resolve it to something
                            context.trace.record(
                                BindingContext.REFERENCE_TARGET,
                                attr.key,
                                attrsOfSameKey.first().descriptor
                            )

                            // we can add all of the possible key targets here so that the user can Command+Click to see the list of
                            // possible values
                            attrsOfSameKey.forEach {
                                context.trace.recordAttributeKeyRef(exprToReportOn, it.descriptor)
                            }
                        }

                        val diagnostic = when {
                            attrsOfSameKey.isNotEmpty() && valueType != null ->
                                ComposeErrors.MISMATCHED_ATTRIBUTE_TYPE.on(
                                    exprToReportOn,
                                    valueType,
                                    attrsOfSameKey.map { it.type }.toSet()
                                )
                            attrsOfSameKey.isEmpty() && valueType != null ->
                                ComposeErrors.UNRESOLVED_ATTRIBUTE_KEY.on(
                                    exprToReportOn,
                                    descriptors,
                                    attr.name,
                                    valueType
                                )
                            attrsOfSameKey.isNotEmpty() && valueType == null
                                    && !valueIsUnresolvedRef ->
                                ComposeErrors.MISMATCHED_ATTRIBUTE_TYPE.on(
                                    exprToReportOn,
                                    ErrorUtils.createErrorType("???"),
                                    attrsOfSameKey.map { it.type }.toSet()
                                )
                            else ->
                                ComposeErrors.UNRESOLVED_ATTRIBUTE_KEY_UNKNOWN_TYPE.on(
                                    exprToReportOn,
                                    descriptors,
                                    attr.name
                                )
                        }

                        context.trace.reportFromPlugin(diagnostic,
                            ComposeDefaultErrorMessages
                        )
                    }
                }
            }
        }

        if (missingRequiredAttributes.isNotEmpty()) {
            missingRequiredAttributes
                .filter { !it.hasChildrenAnnotation() }
                .ifNotEmpty {
                    ComposeErrors.MISSING_REQUIRED_ATTRIBUTES.report(
                        context,
                        tagExpressions,
                        this
                    )
                }
            missingRequiredAttributes
                .filter { it.hasChildrenAnnotation() }
                .ifNotEmpty {
                    ComposeErrors.MISSING_REQUIRED_CHILDREN.report(
                        context,
                        tagExpressions,
                        first().typeAsAttribute()
                    )
                }
        }

        // for each attribute we've consumed, we want to go through and call `checkType` so that the type system can flow through to
        // all of the attributes with the right type information (now that we know what types the attributes should have).
        for (name in usedAttributes) {
            val expr = attrInfos[name]?.value ?: continue
            var type = usedAttributeNodes.find { it.name == name }?.type
            if (type == null && name == CHILDREN_KEY) {
                type = functionType().makeComposable(module)
            }

            facade.checkType(
                expr,
                context.replaceExpectedType(type)
            )
        }

        return ResolvedKtxElementCall(
            usedAttributes = usedAttributeNodes,
            unusedAttributes = unusedAttributes.keys.toList(),
            emitOrCall = emitOrCall,
            getComposerCall = composerResolvedCall,
            emitSimpleUpperBoundTypes = emitSimpleUpperBoundTypes,
            emitCompoundUpperBoundTypes = emitCompoundUpperBoundTypes,
            infixOrCall = infixOrCall,
            attributeInfos = attrInfos
        )
    }

    private fun resolveAttributeCandidatesGivenNameAndNode(
        node: EmitOrCallNode,
        name: String,
        context: ExpressionTypingContext
    ): List<AttributeMeta> {
        val setterName = Name.identifier(
            ComposeUtils.setterMethodFromPropertyName(
                name
            )
        )
        val fakeSetterExpr = psiFactory.createSimpleName(setterName.asString())
        val fakePropertyExpr = psiFactory.createSimpleName(name)
        val contextToUse = context.replaceCollectAllCandidates(true)
        val resolvedCalls = node.resolvedCalls()

        val params = resolvedCalls
            .flatMap { it.resultingDescriptor.valueParameters }
            .filter { it.name.asString() == name }
            .mapNotNull {
                AttributeMeta(
                    name = name,
                    type = it.type,
                    isChildren = it.hasChildrenAnnotation(),
                    descriptor = it
                )
            }

        val types = resolvedCalls
            .mapNotNull { it.resultingDescriptor.returnType }
            .filter { !it.isUnit() }

        // setters, including extension setters
        val setters = types
            .flatMap { type ->
                val call = makeCall(
                    callElement = fakeSetterExpr,
                    calleeExpression = fakeSetterExpr,
                    receiver = TransientReceiver(type)
                )

                callResolver.resolveCallWithGivenName(
                    BasicCallResolutionContext.create(
                        contextToUse,
                        call,
                        CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                        DataFlowInfoForArgumentsImpl(contextToUse.dataFlowInfo, call)
                    ),
                    call,
                    fakeSetterExpr,
                    setterName
                ).allCandidates ?: emptyList()
            }
            .mapNotNull { it.resultingDescriptor as? SimpleFunctionDescriptor }
            .mapNotNull {
                when {
                    it.valueParameters.size != 1 -> null
                    it.returnType?.isUnit() == false -> null
                    else -> AttributeMeta(
                        name = name,
                        type = it.valueParameters.first().type,
                        isChildren = it.hasChildrenAnnotation(),
                        descriptor = it
                    )
                }
            }

        val properties = types
            .flatMap { type ->
                val call = CallMaker.makePropertyCall(
                    TransientReceiver(type),
                    null,
                    fakePropertyExpr
                )

                val contextForVariable = BasicCallResolutionContext.create(
                    contextToUse,
                    call,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
                )

                callResolver.resolveSimpleProperty(contextForVariable).allCandidates ?: emptyList()
            }
            .mapNotNull { it.resultingDescriptor as? PropertyDescriptor }
            .map {
                AttributeMeta(
                    name = name,
                    type = it.type,
                    isChildren = it.hasChildrenAnnotation(),
                    descriptor = it
                )
            }

        return params + setters + properties
    }

    // pure. check where used
    private fun shouldMemoizeResult(resolvedCall: ResolvedCall<*>): Boolean {
        val descriptor = resolvedCall.resultingDescriptor
        val returnType = descriptor.returnType ?: return false
        return returnType.shouldMemoizeType() || descriptor.hasStatefulAnnotation()
    }

    private fun KotlinType.shouldMemoizeType(): Boolean {
        if (isUnit() || isAnyOrNullableAny()) return false
        val typeDescriptor = constructor.declarationDescriptor
        return hasStatefulAnnotation() ||
                supertypes().any { it.shouldMemoizeType() } ||
                (typeDescriptor?.hasStatefulAnnotation() ?: false)
    }

    private fun resolveTagValidations(
        kind: ComposerCallKind,
        step: ResolveStep,
        resolvedCall: ResolvedCall<*>,
        receiverScope: KotlinType,
        context: ExpressionTypingContext
    ): List<ValidatedAssignment> {
        if (step !is ResolveStep.Root) return emptyList()
        val descriptor = resolvedCall.resultingDescriptor
        when (resolvedCall.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> {
                val receiver = resolvedCall.dispatchReceiver as? ExpressionReceiver
                    ?: return emptyList()
                return listOf(
                    ValidatedAssignment(
                        validationType = ValidationType.CHANGED,
                        validationCall = resolveValidationCall(
                            kind = kind,
                            validationType = ValidationType.CHANGED,
                            attrType = receiver.type,
                            expressionToReportErrorsOn = receiver.expression,
                            receiverScope = receiverScope,
                            assignmentReceiverScope = null,
                            context = context
                        ).first,
                        assignment = null,
                        assignmentLambda = null,
                        attribute = AttributeNode(
                            name = TAG_KEY,
                            isStatic = false,
                            type = receiver.type,
                            expression = receiver.expression,
                            descriptor = descriptor
                        )
                    )
                )
            }
            else -> return emptyList()
        }
    }

    // Loop through all of the validated assignments for the child call and create validations for the parent call.
    // The validations from the child call should be converted into CHANGED validations because if they were set/update we
    // don't want to do anything but we do want changes in them to invalidate the whole group.
    private fun collectValidations(
        kind: ComposerCallKind,
        current: List<ValidatedAssignment>,
        children: List<AttributeNode>,
        expression: KtExpression,
        invalidReceiverScope: KotlinType,
        attributes: Map<String, AttributeInfo>,
        context: ExpressionTypingContext
    ): List<ValidatedAssignment> {
        val result = mutableMapOf<String, ValidatedAssignment>()

        current.forEach {
            result[it.attribute.name] = it
        }

        children.forEach {
            if (result.containsKey(it.name)) return@forEach
            val attr = attributes[it.name] ?: error("did not find attribute")
            result[it.name] = it.asChangedValidatedAssignment(
                kind = kind,
                expressionToReportErrorsOn = attr.key ?: expression,
                receiverScope = invalidReceiverScope,
                context = context
            )
        }

        return result.values.toList()
    }

    private fun resolveChild(
        expression: KtExpression,
        resolveStep: ResolveStep,
        call: Call,
        attributes: Map<String, AttributeInfo>,
        usedAttributes: MutableSet<String>,
        missingRequiredAttributes: MutableList<DeclarationDescriptor>,
        context: ExpressionTypingContext,
        recurseOnUnresolved: Boolean
    ): EmitOrCallNode {
        if (!resolveStep.canRecurse()) {
            return when (resolveStep) {
                is ResolveStep.Root -> error("should never happen")
                is ResolveStep.Nested -> {
                    resolveStep.constructNonMemoizedCallLinkedList()?.apply {
                        nextCall = resolveStep.errorNode
                    } ?: resolveStep.errorNode!!
                }
            }
        }
        val tmpForCandidates = TemporaryTraceAndCache.create(
            context, "trace to resolve ktx element", expression
        )
        val results = getCandidates(
            resolveStep,
            call,
            context.replaceTraceAndCache(tmpForCandidates)
        )

        if (results.allCandidates?.size == 0) {
            return when (resolveStep) {
                is ResolveStep.Root -> {
                    // if the root tag failed to resolve to anything, then the tag isn't even callable. The call resolver will
                    // add useful diagnostics in this case that we'd like to use, but it will only do it when we are not in
                    // the "collectAllCandidates" mode. We just call `getCandidates` again to put all of the diagnostics on the element
                    // that we want.
                    getCandidates(resolveStep, call, context, collectAllCandidates = false)
                    ErrorNode.NonCallableRoot()
                }
                is ResolveStep.Nested -> {
                    val error = ErrorNode.NonEmittableNonCallable(resolveStep.calleeType)
                    resolveStep.constructNonMemoizedCallLinkedList()?.apply { nextCall = error }
                        ?: error
                }
            }
        }

        // TODO(lmr): we could have an optimization for results.isSuccess and attributes.size == 0 here

        val resolveInfos = results.allCandidates!!.mapNotNull { result ->
            val tmpForCandidate = TemporaryTraceAndCache.create(
                context, "trace to resolve ktx element", expression
            )

            var trace: BindingTrace = tmpForCandidate.trace

            if (resolveStep is ResolveStep.Root) {
                resolveStep.closeExpr?.let {
                    trace = referenceCopyingTrace(
                        resolveStep.openExpr,
                        resolveStep.closeExpr,
                        tmpForCandidate.trace
                    )
                }
                // not sure why this is needed, but it is
                copyReferences(
                    fromTrace = tmpForCandidate.trace,
                    toTrace = trace,
                    element = resolveStep.openExpr
                )
            }

            val candidateContext = context
                .replaceTraceAndCache(tmpForCandidate)
                .replaceBindingTrace(trace)

            val attrsUsedInCall = mutableSetOf<String>()

            val attrsUsedInSets = mutableSetOf<String>()

            val subMissingRequiredAttributes = mutableListOf<DeclarationDescriptor>()

            val usedAttributeInfos = mutableListOf<TempParameterInfo>()

            val candidateResults = resolveCandidate(
                resolveStep,
                result,
                call,
                attributes,
                attrsUsedInCall,
                usedAttributeInfos,
                subMissingRequiredAttributes,
                candidateContext
            )

            if (candidateResults.isNothing) return@mapNotNull TempResolveInfo(
                false,
                tmpForCandidate,
                (attributes - attrsUsedInCall).keys,
                attrsUsedInCall,
                subMissingRequiredAttributes
            ) {
                ErrorNode.ResolveError(results)
            }

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
                                            val attr =
                                                attributes[it.valueParameter.name.asString()]
                                                    ?: return@forEach
                                            val key = attr.key ?: return@forEach
                                            val type =
                                                facade.getTypeInfo(
                                                    attr.value,
                                                    candidateContext
                                                ).type ?: return@forEach
                                            candidateContext.trace.reportFromPlugin(
                                                ComposeErrors.MISMATCHED_ATTRIBUTE_TYPE.on(
                                                    key,
                                                    type,
                                                    listOfNotNull(it.valueParameter.type)
                                                ),
                                                ComposeDefaultErrorMessages
                                            )
                                        }
                                        ArgumentMatchStatus.MATCH_MODULO_UNINFERRED_TYPES -> {
                                            val attr = attributes[it.valueParameter.name.asString()]
                                                ?: return@forEach
                                            val key = attr.key ?: return@forEach
                                            val type =
                                                facade.getTypeInfo(
                                                    attr.value,
                                                    candidateContext
                                                ).type ?: return@forEach

                                            candidateContext.trace.reportFromPlugin(
                                                ComposeErrors.MISMATCHED_INFERRED_ATTRIBUTE_TYPE.on(
                                                    key,
                                                    type,
                                                    listOfNotNull(it.valueParameter.type)
                                                ),
                                                ComposeDefaultErrorMessages
                                            )
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
                                    if (!recurseOnUnresolved) {
                                        return@mapNotNull TempResolveInfo(
                                            false,
                                            tmpForCandidate,
                                            (attributes - attrsUsedInCall).keys,
                                            attrsUsedInCall,
                                            subMissingRequiredAttributes
                                        ) {
                                            ErrorNode.ResolveError(
                                                results
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE,
                    OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES,
                    OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER -> {
                        if (!recurseOnUnresolved) {
                            return@mapNotNull TempResolveInfo(
                                false,
                                tmpForCandidate,
                                (attributes - attrsUsedInCall).keys,
                                attrsUsedInCall,
                                subMissingRequiredAttributes
                            ) {
                                ErrorNode.ResolveError(results)
                            }
                        } else {
                            candidateContext.trace.recordFailedCandidates(
                                mainElement,
                                results.allCandidates
                            )
                        }
                    }
                    else -> {
                        error("new kind of resolution problem. figure out why this happened...")
                    }
                }
            }

            if (
                !descriptorsEqualWithSubstitution(
                    resolvedCall.semanticCall.candidateDescriptor.original,
                    result.semanticCall.candidateDescriptor.original,
                    false
                )
            ) {
                // if this happens, we've likely resolved the candidate to one of the other candidates, and we don't want that.
                // we fail in this case, since it will succeed once we actually process the candidate that this resolved to.
                return@mapNotNull TempResolveInfo(
                    false,
                    tmpForCandidate,
                    (attributes - attrsUsedInCall).keys,
                    attrsUsedInCall,
                    subMissingRequiredAttributes
                ) {
                    ErrorNode.ResolveError(results)
                }
            }

            val returnType = resolvedCall.resultingDescriptor.returnType ?: builtIns.unitType

            val isStaticCall = isStaticTag(
                resolveStep,
                resolvedCall,
                candidateContext
            )

            val shouldMemoizeCtor = returnType.isEmittable() || shouldMemoizeResult(resolvedCall)

            if (returnType.isUnit()) {
                // bottomed out

                // it is important to pass in "result" here and not "resolvedCall" since "result" is the one that will have
                // the composable annotation on it in the case of lambda invokes
                if (
                    !composableAnnotationChecker.shouldInvokeAsTag(
                        candidateContext.trace, result
                    )
                ) {
                    candidateContext.trace.reportFromPlugin(
                        ComposeErrors.NON_COMPOSABLE_INVOCATION.on(
                            expression,
                            "Lambda variable",
                            resolvedCall.semanticCall.resultingDescriptor
                        ),
                        ComposeDefaultErrorMessages
                    )
                }

                return@mapNotNull TempResolveInfo(
                    true, // TODO(lmr): valid
                    tmpForCandidate,
                    (attributes - attrsUsedInCall).keys,
                    attrsUsedInCall,
                    subMissingRequiredAttributes
                ) {
                    val composerCall = resolveComposerCall(
                        constructedType = null, // or should we pass in Unit here?
                        expressionToReportErrorsOn = expression,
                        context = candidateContext
                    )

                    val invalidReceiverScope = composerCall
                        ?.resultingDescriptor
                        ?.valueParameters
                        ?.first { it.name == KtxNameConventions.CALL_INVALID_PARAMETER }
                        ?.type
                        ?.getReceiverTypeFromFunctionType()
                        ?: ErrorUtils.createErrorType("???")

                    val tagValidations = resolveTagValidations(
                        kind = ComposerCallKind.CALL,
                        step = resolveStep,
                        resolvedCall = resolvedCall,
                        receiverScope = invalidReceiverScope,
                        context = candidateContext
                    )

                    val pivotals = resolvePivotalAttributes(
                        attributes,
                        attrsUsedInCall,
                        usedAttributeInfos,
                        emptyList(),
                        returnType
                    )

                    val nonMemoizedCall = NonMemoizedCallNode(
                        resolvedCall = resolvedCall,
                        params = resolvedCall.buildParamsFromAttributes(attributes),
                        applyCall = null,
                        applyLambdaDescriptor = null,
                        applyLambdaType = null,
                        postAssignments = emptyList(),
                        nextCall = null
                    )

                    if (resolveStep is ResolveStep.Root) {
                        MemoizedCallNode(
                            memoize = ComposerCallInfo(
                                context = candidateContext,
                                composerCall = composerCall,
                                pivotals = pivotals,
                                joinKeyCall = joinKeyCall,
                                ctorCall = null,
                                ctorParams = emptyList(),
                                validations = collectValidations(
                                    kind = ComposerCallKind.CALL,
                                    current = tagValidations,
                                    children = nonMemoizedCall.consumedAttributes(),
                                    expression = expression,
                                    attributes = attributes,
                                    invalidReceiverScope = invalidReceiverScope,
                                    context = candidateContext
                                )
                            ),
                            call = resolveStep.constructNonMemoizedCallLinkedList(nonMemoizedCall)
                        )
                    } else {
                        resolveStep.constructNonMemoizedCallLinkedList(nonMemoizedCall)
                    }
                }
            }

            var inlineChildren: KtExpression? = null
            if (returnType.isEmittable() == true) {

                val composerCall = resolveComposerEmit(
                    constructedType = returnType,
                    hasBody = attributes.contains(CHILDREN_KEY),
                    implicitCtorTypes = resolvedCall.call.valueArguments.mapNotNull {
                        (it as? ImplicitCtorValueArgument)?.type
                    },
                    expressionToReportErrorsOn = expression,
                    context = candidateContext
                ) ?: return@mapNotNull TempResolveInfo(
                    false,
                    tmpForCandidate,
                    (attributes - attrsUsedInCall - attrsUsedInSets).keys,
                    attrsUsedInCall + attrsUsedInSets,
                    subMissingRequiredAttributes
                ) {
                    ErrorNode.ResolveError(results)
                }

                if (attributes.contains(CHILDREN_KEY) && returnType.isCompoundEmittable()) {
                    attrsUsedInSets.add(CHILDREN_KEY)
                    inlineChildren = attributes.get(CHILDREN_KEY)!!.value
                }

                val updateReceiverScope = composerCall
                    .resultingDescriptor
                    .valueParameters
                    .first { it.name == KtxNameConventions.EMIT_UPDATER_PARAMETER }
                    .type
                    .getReceiverTypeFromFunctionType() ?: error("Expected receiver type")

                val setterValidations = resolveAllSetAttributes(
                    kind = ComposerCallKind.EMIT,
                    expressionToReportErrorsOn = expression,
                    receiverScope = updateReceiverScope,
                    type = returnType,
                    attributes = attributes,
                    attributesUsedInCall = attrsUsedInCall,
                    consumedAttributes = attrsUsedInSets,
                    missingRequiredAttributes = subMissingRequiredAttributes,
                    shouldMemoizeCtor = shouldMemoizeCtor,
                    context = candidateContext
                )

                val pivotals = resolvePivotalAttributes(
                    attributes,
                    attrsUsedInCall,
                    usedAttributeInfos,
                    setterValidations,
                    returnType
                )

                return@mapNotNull TempResolveInfo(
                    true,
                    tmpForCandidate,
                    (attributes - attrsUsedInCall - attrsUsedInSets).keys,
                    attrsUsedInCall + attrsUsedInSets,
                    subMissingRequiredAttributes
                ) {
                    EmitCallNode(
                        memoize = ComposerCallInfo(
                            context = candidateContext,
                            composerCall = composerCall,
                            pivotals = pivotals,
                            joinKeyCall = joinKeyCall,
                            ctorCall = resolvedCall,
                            ctorParams = resolvedCall.buildParamsFromAttributes(attributes),
                            validations = setterValidations
                        ),
                        inlineChildren = inlineChildren
                    )
                }
            }

            val composerCall = resolveComposerCall(
                constructedType = if (shouldMemoizeCtor) returnType else null,
                expressionToReportErrorsOn = expression,
                context = candidateContext
            )

            // the "invalid" lambda is at a different argument index depending on whether or not there is a "ctor" param.
            val invalidReceiverScope = composerCall
                ?.resultingDescriptor
                ?.valueParameters
                ?.first { it.name == KtxNameConventions.CALL_INVALID_PARAMETER }
                ?.type
                ?.getReceiverTypeFromFunctionType()
                ?: ErrorUtils.createErrorType("???")

            val tagValidations = resolveTagValidations(
                kind = ComposerCallKind.CALL,
                step = resolveStep,
                resolvedCall = resolvedCall,
                receiverScope = invalidReceiverScope,
                context = candidateContext
            )

            val setterValidations = resolveAllSetAttributes(
                kind = ComposerCallKind.CALL,
                expressionToReportErrorsOn = expression,
                receiverScope = invalidReceiverScope,
                type = returnType,
                attributes = attributes,
                attributesUsedInCall = attrsUsedInCall,
                consumedAttributes = attrsUsedInSets,
                missingRequiredAttributes = subMissingRequiredAttributes,
                shouldMemoizeCtor = shouldMemoizeCtor,
                context = candidateContext
            )

            val pivotals = resolvePivotalAttributes(
                attributes,
                attrsUsedInCall,
                usedAttributeInfos,
                setterValidations,
                returnType
            )

            val attrsUsedInFollowingCalls = mutableSetOf<String>()

            // TODO(lmr): clean this up a little bit
            val postAssignments = if (!shouldMemoizeCtor) {
                (tagValidations + setterValidations)
                    .filter { it.validationType != ValidationType.CHANGED }
            } else emptyList()

            val allValidations = if (!shouldMemoizeCtor) {
                (tagValidations + setterValidations).map {
                    when (it.validationType) {
                        ValidationType.CHANGED -> it
                        ValidationType.UPDATE,
                        ValidationType.SET -> ValidatedAssignment(
                            validationType = ValidationType.CHANGED,
                            validationCall = resolveValidationCall(
                                kind = ComposerCallKind.CALL,
                                validationType = ValidationType.CHANGED,
                                attrType = it.attribute.type,
                                expressionToReportErrorsOn = expression,
                                receiverScope = invalidReceiverScope,
                                assignmentReceiverScope = null,
                                context = context
                            ).first,
                            assignment = null,
                            attribute = it.attribute,
                            assignmentLambda = null
                        )
                    }
                }
            } else tagValidations + setterValidations

            val applyCall = if (!shouldMemoizeCtor) {
                resolveApplyCallForType(returnType, candidateContext)
            } else null

            val applyLambdaType = if (!shouldMemoizeCtor)
                functionType(receiverType = returnType)
            else null
            val applyLambdaDescriptor = if (!shouldMemoizeCtor) {
                createFunctionDescriptor(
                    applyLambdaType!!,
                    candidateContext
                )
            } else null

            val nonMemoizedCall = NonMemoizedCallNode(
                resolvedCall = resolvedCall,
                params = resolvedCall.buildParamsFromAttributes(attributes),
                applyCall = applyCall,
                applyLambdaDescriptor = applyLambdaDescriptor,
                postAssignments = postAssignments,
                applyLambdaType = applyLambdaType,
                nextCall = null
            )

            val childCall = resolveChild(
                expression,
                resolveStep.recurse(
                    calleeType = returnType,
                    nonMemoizedCall = if (shouldMemoizeCtor) null else nonMemoizedCall,
                    isStaticCall = isStaticCall,
                    resolvedCall = resolvedCall,
                    specifiedAttributes = attrsUsedInCall + attrsUsedInSets
                ),
                makeCall(
                    psiFactory.createSimpleName("invoke"),
                    dispatchReceiver = TransientReceiver(returnType)
                ),
                attributes,
                attrsUsedInFollowingCalls,
                subMissingRequiredAttributes,
                candidateContext,
                recurseOnUnresolved
            )

            val subUsedAttributes = attrsUsedInCall + attrsUsedInSets + attrsUsedInFollowingCalls

            return@mapNotNull TempResolveInfo(
                true, // TODO(lmr): valid
                tmpForCandidate,
                (attributes - subUsedAttributes).keys,
                subUsedAttributes,
                subMissingRequiredAttributes
            ) {
                if (shouldMemoizeCtor || resolveStep is ResolveStep.Root) {
                    MemoizedCallNode(
                        memoize = ComposerCallInfo(
                            context = candidateContext,
                            composerCall = composerCall,
                            pivotals = pivotals,
                            joinKeyCall = joinKeyCall,
                            ctorCall = if (shouldMemoizeCtor)
                                nonMemoizedCall.resolvedCall
                            else null,
                            ctorParams = if (shouldMemoizeCtor)
                                nonMemoizedCall.params
                            else emptyList(),
                            validations = collectValidations(
                                kind = ComposerCallKind.CALL,
                                current = allValidations,
                                children = childCall.consumedAttributes(),
                                expression = expression,
                                attributes = attributes,
                                invalidReceiverScope = invalidReceiverScope,
                                context = candidateContext
                            )
                        ),
                        call = childCall
                    )
                } else {
                    childCall
                }
            }
        }
            .sortedWith(Comparator { a, b ->
                // TODO(lmr): should we also use missingRequiredAttributes to sort here?
                if (a.attributesLeft != b.attributesLeft) {
                    a.attributesLeft - b.attributesLeft
                } else {
                    (if (a.valid) 0 else 1) - (if (b.valid) 0 else 1)
                }
            }).toList()

        val result = resolveInfos.first()

        if (!recurseOnUnresolved && result.attributesLeft > 0 &&
            !results.allCandidates.isNullOrEmpty()) {
            // Looking up all possible combinations of resolved calls to satisfy a KTX element can be expensive, so normally we try to do as
            // few lookups as possible. But if we failed (ie, there is for sure an error), then we go ahead and run it again with a flag
            // set that explores all options so that we can provide the most meaningful diagnostics.
            return resolveChild(
                expression,
                resolveStep,
                call,
                attributes,
                usedAttributes,
                missingRequiredAttributes,
                context,
                recurseOnUnresolved = true
            )
        }

        val resultNode = result.build()
        usedAttributes.addAll(result.usedAttributes)
        missingRequiredAttributes.addAll(result.missingRequiredAttributes)
        result.trace.commit()

        if (resolveInfos.size > 1) {
            val nextBest = resolveInfos[1]
            if (result.attributesLeft == 0 && result.attributesLeft
                == nextBest.attributesLeft && nextBest.valid) {
                val nextNode = nextBest.build()
                val primaryCalls = listOfNotNull(
                    resultNode.primaryCall,
                    nextNode.primaryCall
                ).distinctBy { it.semanticCall.resultingDescriptor }
                if (primaryCalls.size > 1) {
                    ComposeErrors.AMBIGUOUS_KTX_CALL.report(
                        context,
                        tagExpressions,
                        primaryCalls
                    )
                }
            }
        }
        return resultNode
    }

    private fun resolvePivotalAttributes(
        attributes: Map<String, AttributeInfo>,
        attrsUsedInCall: Set<String>,
        callParamInfos: List<TempParameterInfo>,
        validations: List<ValidatedAssignment>,
        returnType: KotlinType?
    ): List<AttributeNode> {
        val result = mutableListOf<AttributeNode>()

        if (returnType == null || returnType.isUnit()) {
            return callParamInfos
                .filter { it.descriptor.hasPivotalAnnotation() }
                .map {
                    AttributeNode(
                        name = it.attribute.name,
                        descriptor = it.descriptor,
                        type = it.type,
                        expression = it.attribute.value,
                        isStatic = false
                    )
                }
        }

        val validationSet = validations.map { it.attribute.name }.toSet()

        // if you were in the ctor call but not in the sets, you *have* to be pivotal
        for (info in callParamInfos) {
            if (validationSet.contains(info.attribute.name)) continue
            val attribute = attributes[info.attribute.name] ?: continue
            result.add(
                AttributeNode(
                    name = info.attribute.name,
                    descriptor = info.descriptor,
                    type = info.type,
                    expression = attribute.value,
                    isStatic = false
                )
            )
        }

        // There are additional cases where attributes can be pivotal:
        //   1. It is annotated as @Pivotal
        //   2. It is a `val` ctor parameter
        for (assignment in validations) {
            val attribute = assignment.attribute
            val name = attribute.name
            val descriptor = attribute.descriptor

            if (descriptor.hasPivotalAnnotation()) {
                result.add(
                    AttributeNode(
                        name = name,
                        descriptor = descriptor,
                        type = attribute.type,
                        expression = attribute.expression,
                        isStatic = false
                    )
                )
                continue
            }
            if (descriptor is PropertyDescriptor &&
                attrsUsedInCall.contains(name) && !descriptor.isVar) {
                result.add(
                    AttributeNode(
                        name = name,
                        descriptor = descriptor,
                        type = attribute.type,
                        expression = attribute.expression,
                        isStatic = false
                    )
                )
                continue
            }
        }

        return result
    }

    private fun ResolvedCall<*>.buildParamsFromAttributes(
        attributes: Map<String, AttributeInfo>
    ): List<ValueNode> {
        val possbileChildrenParam = valueArguments.keys.possibleChildrenParameter
        return valueArguments.map { (param, value) ->
            val name = param.name.asString()
            var type = param.type
            var attr = attributes[name]

            if (param.hasChildrenAnnotation() || (param == possbileChildrenParam && attr == null)) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    attr = childrenAttr
                }
                if (param.isComposableFromChildrenAnnotation()) {
                    type = type.makeComposable(module)
                }
            }

            if (value is DefaultValueArgument) {
                return@map DefaultValueNode(
                    name = name,
                    descriptor = param,
                    type = type
                )
            }

            if (attr == null && isImplicitConstructorParam(param, resultingDescriptor)) {
                return@map ImplicitCtorValueNode(
                    name = name,
                    descriptor = param,
                    type = type
                )
            }

            if (attr == null) {
                error("Couldn't find attribute but expected to. param=$param name=$name")
            }

            AttributeNode(
                name = attr.name,
                isStatic = false,
                descriptor = param,
                type = type,
                expression = attr.value
            )
        }
    }

    // pure, can be moved out
    private fun AttributeNode.asChangedValidatedAssignment(
        kind: ComposerCallKind,
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType,
        context: ExpressionTypingContext
    ): ValidatedAssignment {
        val validationCall = resolveValidationCall(
            kind = kind,
            validationType = ValidationType.CHANGED,
            attrType = type,
            expressionToReportErrorsOn = expressionToReportErrorsOn,
            receiverScope = receiverScope,
            assignmentReceiverScope = null,
            context = context
        ).first

        return ValidatedAssignment(
            validationType = ValidationType.CHANGED,
            validationCall = validationCall,
            attribute = this,
            assignment = null,
            assignmentLambda = null
        )
    }

    private fun resolveAllSetAttributes(
        kind: ComposerCallKind,
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType,
        type: KotlinType?,
        attributes: Map<String, AttributeInfo>,
        attributesUsedInCall: Set<String>,
        consumedAttributes: MutableSet<String>,
        missingRequiredAttributes: MutableList<DeclarationDescriptor>,
        shouldMemoizeCtor: Boolean,
        context: ExpressionTypingContext
    ): List<ValidatedAssignment> {
        if (type == null) return emptyList()
        val results = mutableListOf<ValidatedAssignment>()
        var children: AttributeInfo? = null

        val tempForAttributes = TemporaryTraceAndCache.create(
            context, "temp for attributes", expressionToReportErrorsOn
        )

        val tempForValidations = TemporaryTraceAndCache.create(
            context, "temp for validations", expressionToReportErrorsOn
        )

        for ((name, attribute) in attributes) {
            if (name == TAG_KEY) continue
            if (name == CHILDREN_KEY) {
                children = attribute
                continue
            }
            val keyExpr = attribute.key ?: error("key expected")

            val expectedTypes = mutableListOf<KotlinType>()

            var resolvedCall: ResolvedCall<*>? = null

            // NOTE(lmr): A ktx element that has access (like its a recursive call or a nested class) to the private property
            // of the tag will be able to set it as an attribute...  I'm not sure if that'a s good thing or not, but unless we
            // do something extra, that is indeed possible. Perhaps it's something we should look into.

            if (resolvedCall == null) {
                resolvedCall = resolveAttributeAsSetter(
                    type,
                    attribute.name,
                    keyExpr,
                    attribute.value,
                    attribute.isPunned,
                    expectedTypes,
                    context.replaceTraceAndCache(tempForAttributes)
                )
            }

            if (resolvedCall == null) {
                resolvedCall = resolveAttributeAsProperty(
                    type,
                    keyExpr,
                    attribute.value,
                    attribute.isPunned,
                    expectedTypes,
                    context.replaceTraceAndCache(tempForAttributes)
                )
            }

            if (resolvedCall != null) {

                val validationType = when {
                    !shouldMemoizeCtor && attributesUsedInCall.contains(name) ->
                        ValidationType.CHANGED
                    attributesUsedInCall.contains(name) -> ValidationType.UPDATE
                    else -> ValidationType.SET
                }

                val descriptor = resolvedCall.resultingDescriptor

                val attrType = when (descriptor) {
                    is FunctionDescriptor -> descriptor.valueParameters.firstOrNull()?.type
                    is PropertyDescriptor -> descriptor.type
                    else -> null
                } ?: continue

                val (validationCall, lambdaDescriptor) = resolveValidationCall(
                    kind = kind,
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
                        attribute = AttributeNode(
                            name = name,
                            expression = attribute.value,
                            type = attrType,
                            descriptor = resolvedCall.resultingDescriptor,
                            isStatic = false
                        ),
                        validationCall = validationCall
                    )
                )
                consumedAttributes.add(name)
            }
        }

        if (children != null) {
            val childrenExpr = children.value

            var resolvedCall: ResolvedCall<*>? = null

            for (descriptor in getChildrenDescriptors(type, attributesUsedInCall, context)) {
                if (resolvedCall != null) break

                when (descriptor) {
                    is PropertyDescriptor -> {
                        resolvedCall = resolveChildrenAsProperty(
                            type,
                            descriptor,
                            childrenExpr,
                            context.replaceTraceAndCache(tempForAttributes),
                            attributesUsedInCall.contains(CHILDREN_KEY)
                        )
                    }
                    is SimpleFunctionDescriptor -> {
                        resolvedCall = resolveChildrenAsSetter(
                            type,
                            descriptor,
                            childrenExpr,
                            context.replaceTraceAndCache(tempForAttributes)
                        )
                    }
                }
            }
            if (resolvedCall != null) {

                val descriptor = resolvedCall.resultingDescriptor

                val validationType = when {
                    attributesUsedInCall.contains(CHILDREN_KEY) -> ValidationType.UPDATE
                    else -> ValidationType.SET
                }

                val attrName = when (descriptor) {
                    is SimpleFunctionDescriptor ->
                        ComposeUtils.propertyNameFromSetterMethod(
                            descriptor.name.asString()
                        )
                    is PropertySetterDescriptor -> descriptor.correspondingProperty.name.asString()
                    else -> descriptor.name.asString()
                }

                attributes[attrName]?.let {
                    if (attributesUsedInCall.contains(attrName) &&
                        attributesUsedInCall.contains(CHILDREN_KEY)) {
                        // if they were both already used in the call, then this diagnostic will already have been reported, and we
                        // can ignore it
                    } else {
                        // they are providing a named attribute for a @Children attribute while also providing a children
                        // body. This is illegal.
                        context.trace.reportFromPlugin(
                            ComposeErrors.CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE.on(
                                it.key!!,
                                attrName
                            ),
                            ComposeDefaultErrorMessages
                        )
                    }
                    consumedAttributes.add(attrName)
                }

                val attrType = when (descriptor) {
                    is FunctionDescriptor -> descriptor.valueParameters.firstOrNull()?.type
                        ?: error("Expected single parameter setter")
                    is PropertyDescriptor -> descriptor.type
                    else -> error("Unknown callable type encountered")
                }

                val (validationCall, lambdaDescriptor) = resolveValidationCall(
                    kind = kind,
                    expressionToReportErrorsOn = expressionToReportErrorsOn,
                    receiverScope = receiverScope,
                    assignmentReceiverScope = type,
                    validationType = validationType,
                    attrType = attrType,
                    context = context.replaceTraceAndCache(tempForValidations)
                )

                results.add(
                    ValidatedAssignment(
                        validationType = when {
                            attributesUsedInCall.contains(CHILDREN_KEY) -> ValidationType.UPDATE
                            else -> ValidationType.SET
                        },
                        assignment = resolvedCall,
                        assignmentLambda = lambdaDescriptor,
                        attribute = AttributeNode(
                            name = CHILDREN_KEY,
                            expression = children.value,
                            type = attrType,
                            descriptor = resolvedCall.resultingDescriptor,
                            isStatic = false
                        ),
                        validationCall = validationCall
                    )
                )
                consumedAttributes.add(CHILDREN_KEY)
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
                .filter { !it.hasHiddenAttributeAnnotation() }

            requiredAttributes
                .filter { !consumedAttributes.contains(it.name.asString()) }
                .filter { !it.hasChildrenAnnotation() }
                .ifNotEmpty { missingRequiredAttributes.addAll(this) }

            requiredAttributes
                .filter { it.hasChildrenAnnotation() }
                .filter { !consumedAttributes.contains(it.name.asString()) &&
                        !consumedAttributes.contains(CHILDREN_KEY) }
                .ifNotEmpty { missingRequiredAttributes.addAll(this) }
        }

        tempForAttributes.commit()
        tempForValidations.commit()
        return results
    }

    // pure, can be moved out. used in resolveAllSetAttrs
    private fun getChildrenDescriptors(
        type: KotlinType,
        attributesUsedInCall: Set<String>,
        context: ExpressionTypingContext
    ): List<DeclarationDescriptor> {
        val descriptor = type.constructor.declarationDescriptor
        return when (descriptor) {
            is ClassDescriptor -> {
                // We have to be careful here because if the property was defined as a constructor parameter, then the @Children
                // annotation will be on the parameter descriptor, *not* on the property descriptor. These aren't linked in any real
                // sense at the descriptor level, so we have to find the primary constructor and just use the convention of the
                // parameter named of the constructor being the same as the property.
                val ctorParameters = descriptor.unsubstitutedPrimaryConstructor?.valueParameters
                    ?: emptyList()
                val shouldIncludeCtorParam = attributesUsedInCall.contains(CHILDREN_KEY)
                fun DeclarationDescriptor.isChildrenCtorParam(): Boolean {
                    if (!shouldIncludeCtorParam) return false
                    if (this !is PropertyDescriptor) return false
                    val param = ctorParameters.firstOrNull { p -> name == p.name } ?: return false
                    return param.hasChildrenAnnotation()
                }
                descriptor
                    .unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .filter { it.hasChildrenAnnotation() || it.isChildrenCtorParam() }
                    .filter {
                        // if it's a property descriptor, we are only interested if it has a setter
                        if (it is PropertyDescriptor) it.setter != null else true
                    }
                    .filter {
                        // we want to make sure the descriptor is actually visible from the callsite
                        it is DeclarationDescriptorWithVisibility &&
                                Visibilities.isVisibleIgnoringReceiver(
                                    it,
                                    context.scope.ownerDescriptor
                                )
                    }
            }
            else -> emptyList()
        }
    }

    private fun resolveAttributeAsSetter(
        instanceType: KotlinType,
        name: String,
        keyExpr: KtReferenceExpression,
        valueExpr: KtExpression,
        isPunned: Boolean,
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

        if (isPunned) {
            context.trace.recordAttributeKeyRef(valueExpr, resolvedCall.resultingDescriptor)
        }
        temporaryForFunction.commit()

        return resolvedCall
    }

    private fun resolveAttributeAsProperty(
        instanceType: KotlinType,
        keyExpr: KtSimpleNameExpression,
        valueExpr: KtExpression,
        isPunned: Boolean,
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

        facade.getTypeInfo(
            valueExpr,
            contextToUse
                .replaceExpectedType(expectedType)
                .replaceCallPosition(CallPosition.PropertyAssignment(keyExpr))
        )

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

        if (isPunned) {
            context.trace.recordAttributeKeyRef(valueExpr, resolvedCall.resultingDescriptor)
        }

        temporaryForVariable.commit()

        return resolvedCall
    }

    private fun resolveChildrenAsSetter(
        instanceType: KotlinType,
        childrenDescriptor: SimpleFunctionDescriptor,
        childrenExpr: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val setterName = childrenDescriptor.name

        val valueArguments = listOf(CallMaker.makeValueArgument(childrenExpr))
        val receiver = TransientReceiver(instanceType)
        val call = makeCall(
            childrenExpr,
            valueArguments = valueArguments,
            receiver = receiver,
            calleeExpression = childrenExpr // NOTE(lmr): this seems wrong
        )

        val temporaryForFunction = TemporaryTraceAndCache.create(
            context, "trace to resolve as function call", childrenExpr
        )

        val refExpr = psiFactory.createSimpleName(setterName.identifier)

        val results = callResolver.computeTasksAndResolveCall<FunctionDescriptor>(
            BasicCallResolutionContext.create(
                context.replaceTraceAndCache(temporaryForFunction),
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
            ),
            setterName,
            refExpr,
            NewResolutionOldInference.ResolutionKind.Function
        )

        if (results.isNothing) {
            return null
        }

        if (temporaryForFunction.trace.hasTypeMismatchErrorsOn(childrenExpr)) {
            return null
        }
        // TODO(lmr): should we check isSuccess here or anything like that?

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context)
            ?: return null

        temporaryForFunction.commit()

        facade.getTypeInfo(
            childrenExpr,
            context.replaceExpectedType(
                resolvedCall.resultingDescriptor.valueParameters.first().type
            )
        )
        return resolvedCall
    }

    private fun resolveChildrenAsProperty(
        instanceType: KotlinType,
        propertyDescriptor: PropertyDescriptor,
        childrenExpr: KtExpression,
        context: ExpressionTypingContext,
        shouldIncludeCtorParam: Boolean
    ): ResolvedCall<*>? {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve as local variable or property", childrenExpr
        )

        val receiver = TransientReceiver(instanceType)

        val call = makeCall(
            childrenExpr,
            calleeExpression = childrenExpr,
            receiver = receiver
        )

        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
        )

        val results = callResolver.computeTasksAndResolveCall<PropertyDescriptor>(
            contextForVariable,
            propertyDescriptor.name,
            TracingStrategy.EMPTY,
            NewResolutionOldInference.ResolutionKind.Variable
        )

        if (results.isNothing) {
            return null
        }

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context)
            ?: return null

        val composableFromChildrenAnnotation = (propertyDescriptor.containingDeclaration
                as? ClassDescriptor)?.let { containingDeclaration ->
                val ctorParameters =
                    containingDeclaration.unsubstitutedPrimaryConstructor?.valueParameters
                        ?: emptyList()
                fun DeclarationDescriptor.isComposableChildrenCtorParam(): Boolean {
                    if (!shouldIncludeCtorParam) return false
                    if (this !is PropertyDescriptor) return false
                    val param = ctorParameters.firstOrNull { p -> name == p.name } ?: return false
                    return param.isComposableFromChildrenAnnotation()
                }
                resolvedCall.resultingDescriptor.isComposableFromChildrenAnnotation() ||
                        resolvedCall.resultingDescriptor.isComposableChildrenCtorParam()
            } ?: false

        val expectedType =
            if (composableFromChildrenAnnotation)
                (resolvedCall.resultingDescriptor).type.makeComposable(module)
            else (resolvedCall.resultingDescriptor).type

        facade.getTypeInfo(
            childrenExpr,
            context
                .replaceTraceAndCache(temporaryForVariable)
                .replaceExpectedType(expectedType)
                .replaceCallPosition(CallPosition.PropertyAssignment(null))
        )

        if (temporaryForVariable.trace.hasTypeMismatchErrorsOn(childrenExpr)) {
            return null
        }

        temporaryForVariable.commit()

        return resolvedCall
    }

    private fun resolveCandidate(
        step: ResolveStep,
        candidate: ResolvedCall<FunctionDescriptor>,
        original: Call,
        attributes: Map<String, AttributeInfo>,
        usedAttributes: MutableSet<String>,
        usedAttributeInfos: MutableList<TempParameterInfo>,
        missingRequiredAttributes: MutableList<DeclarationDescriptor>,
        context: ExpressionTypingContext
    ): OverloadResolutionResults<FunctionDescriptor> {
        val valueArguments = mutableListOf<ValueArgument>()

        val referencedDescriptor = candidate.semanticCall.resultingDescriptor

        val dispatchReceiver = candidate.semanticCall.dispatchReceiver ?: candidate.dispatchReceiver

        val stableParamNames = referencedDescriptor.hasStableParameterNames()

        val possibleChildrenParameter =
            candidate.resultingDescriptor.valueParameters.possibleChildrenParameter

        for (param in candidate.resultingDescriptor.valueParameters) {
            val name = param.name.asString()
            val attr = attributes[name]
            var arg: ValueArgument? = null

            if (arg == null && (param.hasChildrenAnnotation() ||
                        param == possibleChildrenParameter)) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    usedAttributes.add(CHILDREN_KEY)
                    var type = param.type
                    if (param.isComposableFromChildrenAnnotation()) {
                        type = type.makeComposable(module)
                    }

                    usedAttributeInfos.add(
                        TempParameterInfo(
                            attribute = childrenAttr,
                            descriptor = param,
                            type = type
                        )
                    )

                    arg = childrenAttr.toValueArgument(name, stableParamNames)

                    if (attr != null) {
                        // they are providing a named attribute for a @Children attribute while also providing a children
                        // body. This is illegal.
                        context.trace.reportFromPlugin(
                            ComposeErrors.CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE.on(
                                attr.key!!,
                                attr.name
                            ),
                            ComposeDefaultErrorMessages
                        )
                        usedAttributes.add(attr.name)
                    }
                }
            }

            if (arg == null && attr != null) {
                usedAttributes.add(name)
                usedAttributeInfos.add(
                    TempParameterInfo(
                        attribute = attr,
                        descriptor = param,
                        type = param.type
                    )
                )
                context.trace.record(BindingContext.REFERENCE_TARGET, attr.key, param)
                if (attr.isPunned) {
                    context.trace.recordAttributeKeyRef(attr.value, param)
                }
                arg = attr.toValueArgument(attr.name, stableParamNames)
            }

            if (arg == null && isImplicitConstructorParam(param, referencedDescriptor)) {
                arg = ImplicitCtorValueArgument(param.type)
            }

            if (arg != null) {
                valueArguments.add(arg)
            } else if (!param.declaresDefaultValue()) {
                // missing required parameter!
                if (dispatchReceiver?.type?.isExtensionFunctionType == true &&
                    param == candidate.resultingDescriptor.valueParameters[0]) {
                    // in this case, the missing parameter is an extension parameter... and it will be provided implicitly. We don't want
                    // to diagnose this as a missing attribute. If it is missing, the call resolver will add a diagnostic anyway.
                } else {
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

        return when (step) {
            is ResolveStep.Root -> callResolver.resolveFunctionCall(contextForVariable)
            is ResolveStep.Nested -> forceResolveCallForInvoke(step.calleeType, contextForVariable)
        }
    }

    private fun getCandidates(
        step: ResolveStep,
        call: Call,
        context: ExpressionTypingContext,
        collectAllCandidates: Boolean = true
    ): OverloadResolutionResults<FunctionDescriptor> {
        val contextForVariable = BasicCallResolutionContext.create(
            context,
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
        ).replaceCollectAllCandidates(collectAllCandidates)

        return when (step) {
            is ResolveStep.Root -> callResolver.resolveFunctionCall(contextForVariable)
            is ResolveStep.Nested -> forceResolveCallForInvoke(step.calleeType, contextForVariable)
        }
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

        val receiverTypeInfo = when (context.trace.get(BindingContext.QUALIFIER, receiverExpr)) {
            null -> facade.getTypeInfo(receiverExpr, currentContext)
            else -> KotlinTypeInfo(null, currentContext.dataFlowInfo)
        }

        // TODO(lmr): inspect jumps and nullability. We cant allow tags that can be null or return early
        val receiverType = receiverTypeInfo.type
            ?: ErrorUtils.createErrorType("Type for " + receiverExpr.text)

        return context.trace.get(BindingContext.QUALIFIER, receiverExpr)
            ?: ExpressionReceiver.create(receiverExpr, receiverType, context.trace.bindingContext)
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

    private fun resolveJoinKey(
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {

        return resolveSubstitutableComposerMethod(
            KtxNameConventions.JOINKEY,
            listOf(
                builtIns.anyType,
                builtIns.anyType
            ),
            null,
            expressionToReportErrorsOn,
            context
        )
    }

    private fun resolveInfixOr(context: ExpressionTypingContext): ResolvedCall<*> {
        val orName = Name.identifier("or")
        val left = psiFactory.createSimpleName("a")
        val right = psiFactory.createSimpleName("b")
        val operator = psiFactory.createSimpleName(orName.identifier)

        context.trace.record(
            BindingContext.EXPRESSION_TYPE_INFO, left, KotlinTypeInfo(
                type = builtIns.booleanType,
                dataFlowInfo = DataFlowInfo.EMPTY,
                jumpOutPossible = false,
                jumpFlowInfo = DataFlowInfo.EMPTY
            )
        )

        context.trace.record(
            BindingContext.EXPRESSION_TYPE_INFO, right, KotlinTypeInfo(
                type = builtIns.booleanType,
                dataFlowInfo = DataFlowInfo.EMPTY,
                jumpOutPossible = false,
                jumpFlowInfo = DataFlowInfo.EMPTY
            )
        )

        return callResolver.resolveCallWithGivenName(
            context,
            makeCall(
                callElement = left,
                calleeExpression = operator,
                receiver = ExpressionReceiver.create(
                    left,
                    builtIns.booleanType,
                    context.trace.bindingContext
                ),
                valueArguments = listOf(CallMaker.makeValueArgument(right))
            ),
            operator,
            Name.identifier("or")
        ).resultingCall
    }

    private fun resolveApplyCallForType(
        type: KotlinType,
        context: ExpressionTypingContext
    ): ResolvedCall<*> {
        val apply = psiFactory.createSimpleName("apply")

        val results = callResolver.resolveCallWithGivenName(
            context,
            makeCall(
                callElement = apply,
                calleeExpression = apply,
                receiver = TransientReceiver(type),
                valueArguments = listOf(
                    makeValueArgument(
                        functionType(receiverType = type),
                        context
                    )
                )
            ),
            apply,
            Name.identifier("apply")
        )
        return results.resultingCall
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

    private fun resolveComposerMethodCandidates(
        element: KtExpression,
        name: Name,
        context: ExpressionTypingContext
    ): Collection<ResolvedCall<*>> {
        val calleeExpression = psiFactory.createSimpleName(name.asString())

        val methodCall = makeCall(
            callElement = element,
            calleeExpression = calleeExpression,
            receiver = TransientReceiver(composerType)
        )

        val contextForVariable = BasicCallResolutionContext.create(
            context,
            methodCall,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, methodCall)
        )

        val results = callResolver.resolveCallWithGivenName(
            // it's important that we use "collectAllCandidates" so that extension functions get included
            contextForVariable.replaceCollectAllCandidates(true),
            methodCall,
            calleeExpression,
            name
        )

        return results.allCandidates ?: emptyList()
    }

    private fun resolveComposerCall(
        constructedType: KotlinType?,
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {

        // call signature is:
        // ==================
        // key: Any, invalid: V.() -> Boolean, block: () -> Unit
        // key: Any, ctor: () -> T, invalid: V.(T) -> Boolean, block: (T) -> Unit

        return resolveSubstitutableComposerMethod(
            KtxNameConventions.CALL,
            listOfNotNull(
                builtIns.anyType,
                constructedType?.let {
                    functionType(returnType = constructedType)
                },
                functionType(
                    parameterTypes = listOfNotNull(constructedType),
                    returnType = builtIns.booleanType
                ),
                functionType(
                    parameterTypes = listOfNotNull(
                        constructedType
                    )
                )
            ),
            constructedType,
            expressionToReportErrorsOn,
            context
        )
    }

    private fun resolveValidationCall(
        kind: ComposerCallKind,
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType,
        assignmentReceiverScope: KotlinType?,
        validationType: ValidationType,
        attrType: KotlinType,
        context: ExpressionTypingContext
    ): Pair<ResolvedCall<*>?, FunctionDescriptor?> {

        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve variable", expressionToReportErrorsOn
        )
        val contextToUse = context.replaceTraceAndCache(temporaryForVariable)

        val name = validationType.name.toLowerCase()
        val includeLambda = validationType != ValidationType.CHANGED

        val calleeExpression = psiFactory.createSimpleName(name)

        // for call:
        // ValidatorType.set(AttrType, (AttrType) -> Unit): Boolean
        // ValidatorType.update(AttrType, (AttrType) -> Unit): Boolean
        // ValidatorType.changed(AttrType): Boolean

        // for emit:
        // ValidatorType.set(AttrType, ElementType.(AttrType) -> Unit): Unit
        // ValidatorType.update(AttrType, ElementType.(AttrType) -> Unit): Unit
        // ValidatorType.changed(AttrType): Unit

        val lambdaType = when {
            includeLambda && kind == ComposerCallKind.EMIT -> functionType(
                parameterTypes = listOf(attrType),
                receiverType = assignmentReceiverScope
            )
            includeLambda && kind == ComposerCallKind.CALL -> functionType(
                parameterTypes = listOf(attrType)
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

        if (results.isSuccess) return results.resultingCall to lambdaDescriptor

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
                    receiver = TransientReceiver(composerType),
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
                    return nextResults.resultingCall to lambdaDescriptor
                }
            }
        }

        return null to null
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
            receiver = TransientReceiver(composerType),
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
                receiver = TransientReceiver(composerType),
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

    private fun resolveVar(
        name: Name,
        expr: KtExpression,
        context: ExpressionTypingContext
    ): OverloadResolutionResults<CallableDescriptor> {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve variable", expr
        )
        val call = makeCall(expr)
        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
        )
        return callResolver.computeTasksAndResolveCall<CallableDescriptor>(
            contextForVariable,
            name,
            TracingStrategy.EMPTY,
            NewResolutionOldInference.ResolutionKind.Variable
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

    // call resolver extension
    private fun forceResolveCallForInvoke(
        calleeType: KotlinType,
        context: BasicCallResolutionContext
    ): OverloadResolutionResults<FunctionDescriptor> {
        val fake = psiFactory.createSimpleName(OperatorNameConventions.INVOKE.asString())
        val expressionReceiver = ExpressionReceiver.create(
            fake,
            calleeType,
            context.trace.bindingContext
        )
        val call = CallTransformer.CallForImplicitInvoke(
            context.call.explicitReceiver, expressionReceiver, context.call,
            false
        )
        val tracingForInvoke = TracingStrategyForInvoke(fake, call, calleeType)
        return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke)
    }

    // callResolver extension
    private fun resolveCallForInvoke(
        context: BasicCallResolutionContext,
        tracing: TracingStrategy
    ): OverloadResolutionResults<FunctionDescriptor> {
        return callResolver.computeTasksAndResolveCall<FunctionDescriptor>(
            context, OperatorNameConventions.INVOKE, tracing,
            NewResolutionOldInference.ResolutionKind.Invoke
        )
    }
}

// move these to naming conventions???
const val CHILDREN_KEY = "<children>"
const val TAG_KEY = "<tag>"

// general utils
// ==============================

private fun ExpressionTypingContext.withThrowawayTrace(expression: KtExpression) =
    replaceTraceAndCache(
        TemporaryTraceAndCache.create(
            this,
            "Throwaway trace and cache",
            expression
        )
    )

private val builtIns = DefaultBuiltIns.Instance

private fun makeCall(
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

private fun KotlinType.satisfiesConstraintsOf(T: TypeParameterDescriptor): Boolean {
    return T.upperBounds.all { isSubtypeOf(it) }
}

private fun KotlinType.satisfiesConstraintsOf(bounds: List<KotlinType>): Boolean {
    return bounds.all { isSubtypeOf(it) }
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

private fun KtExpression?.refExpressions(): List<KtReferenceExpression> = when (this) {
    is KtReferenceExpression -> listOf(this)
    is KtDotQualifiedExpression ->
        selectorExpression.refExpressions() + receiverExpression.refExpressions()
    else -> emptyList()
}

private fun KotlinType.upperBounds(): List<KotlinType> {
    return if (isTypeParameter()) {
        TypeUtils.getTypeParameterDescriptorOrNull(this)?.upperBounds ?: emptyList()
    } else {
        listOf(this)
    }
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

private fun DeclarationDescriptor.typeAsAttribute() = when (this) {
    is PropertyDescriptor -> type
    is ParameterDescriptor -> type
    is SimpleFunctionDescriptor -> valueParameters.first().type
    else -> error("unknown descriptor type")
}

/**
 * This function was copied verbatim from descriptorUtils.kt from ide-common.  For some reason, importing this method into our module
 * would not work and would cause a NoClassDefFound exception.
 */
private fun descriptorsEqualWithSubstitution(
    descriptor1: DeclarationDescriptor?,
    descriptor2: DeclarationDescriptor?,
    checkOriginals: Boolean = true
): Boolean {
    if (descriptor1 == descriptor2) return true
    if (descriptor1 == null || descriptor2 == null) return false
    if (checkOriginals && descriptor1.original != descriptor2.original) return false
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

// trace util
// ========================
private fun referenceCopyingTrace(
    from: KtExpression,
    to: KtExpression,
    trace: TemporaryBindingTrace
): BindingTrace {
    val openTagExpressions = from.refExpressions()
    val closeTagExpressions = to.refExpressions()

    if (openTagExpressions.size != closeTagExpressions.size) return trace

    val elMap = openTagExpressions.zip(closeTagExpressions).toMap()

    val observableTrace = ObservableBindingTrace(trace)

    observableTrace.addHandler(BindingContext.REFERENCE_TARGET) { _, key, value ->
        val otherRefExpr = elMap[key]
        if (otherRefExpr != null) {
            trace.record(
                BindingContext.REFERENCE_TARGET,
                otherRefExpr,
                value
            )
        }
    }

    return observableTrace
}

private fun copyReferences(
    fromTrace: TemporaryBindingTrace,
    toTrace: BindingTrace,
    element: KtExpression
) {
    val references = element.refExpressions()
    val filter = TraceEntryFilter { slice, key ->
        slice === BindingContext.REFERENCE_TARGET && key in references
    }
    fromTrace.addOwnDataTo(toTrace, filter, false)
}

// util classes
// ========================
private class ImplicitCtorValueArgument(val type: KotlinType) : ValueArgument {
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
    val name: String,
    val isPunned: Boolean
)

private sealed class ResolveStep(
    private val attributes: Set<String>,
    private val isValid: Boolean,
    private val trail: IntArray,
    val errorNode: ErrorNode?
) {
    class Root(
        val openExpr: KtExpression,
        val closeExpr: KtExpression?
    ) : ResolveStep(emptySet(), true, intArrayOf(1, 1, 1), null)

    class Nested(
        val calleeType: KotlinType,
        val nonMemoizedCall: NonMemoizedCallNode?,
        val isStaticCall: Boolean,
        val parent: ResolveStep,
        attributes: Set<String>,
        isValid: Boolean,
        trail: IntArray,
        errorNode: ErrorNode?
    ) : ResolveStep(attributes, isValid, trail, errorNode) {
        fun constructNonMemoizedCallLinkedList(): NonMemoizedCallNode? {
            return nonMemoizedCall?.let { parent.constructNonMemoizedCallLinkedList(it) }
        }
    }

    fun recurse(
        calleeType: KotlinType,
        nonMemoizedCall: NonMemoizedCallNode?,
        isStaticCall: Boolean,
        resolvedCall: ResolvedCall<*>,
        specifiedAttributes: Set<String>
    ): ResolveStep {
        val possibleAttributes = resolvedCall.resultingDescriptor.valueParameters.map {
            it.name.asString()
        }.toSet()
        var errorNode: ErrorNode? = null
        // steps in the recursion cannot define attributes that conflict with previous steps
        val intersection = attributes.intersect(possibleAttributes)
        val hasDuplicates = intersection.isNotEmpty()

        if (hasDuplicates) {
            // TODO(lmr): it would be nice if we also grabbed the descriptors that these attributes were on
            errorNode = ErrorNode.RecursionLimitAmbiguousAttributesError(intersection)
        }

        // we require that at least one of the last three steps has had an attribute that was used.
        // we could tweak this. Potentially the "hasDuplicates" test is more than enough to prevent
        // infinite recursion.
        val nextTrail = intArrayOf(trail[1], trail[2], specifiedAttributes.size)
        val trailIsValid = nextTrail.sum() > 0

        if (!trailIsValid) {
            errorNode = ErrorNode.RecursionLimitError()
        }

        return Nested(
            calleeType = calleeType,
            nonMemoizedCall = nonMemoizedCall,
            isStaticCall = isStaticCall,
            parent = this,
            attributes = attributes + possibleAttributes,
            isValid = !hasDuplicates && trailIsValid,
            trail = nextTrail,
            errorNode = errorNode
        )
    }

    fun constructNonMemoizedCallLinkedList(
        nonMemoizedCall: NonMemoizedCallNode
    ): NonMemoizedCallNode {
        var call = nonMemoizedCall
        var node = this
        while (node is Nested) {
            val prevCall = node.nonMemoizedCall ?: break
            node = node.parent
            call = NonMemoizedCallNode(
                resolvedCall = prevCall.resolvedCall,
                params = prevCall.params,
                applyCall = prevCall.applyCall,
                applyLambdaDescriptor = prevCall.applyLambdaDescriptor,
                postAssignments = prevCall.postAssignments,
                applyLambdaType = prevCall.applyLambdaType,
                nextCall = call
            )
        }
        return call
    }

    fun canRecurse(): Boolean = isValid
}

// static checking
// ==========================
private fun isStatic(
    expression: KtExpression,
    context: ExpressionTypingContext,
    expectedType: KotlinType?,
    constantChecker: ConstantExpressionEvaluator
): Boolean {
    val constValue = constantChecker.evaluateExpression(expression, context.trace, expectedType)
    return constValue != null
}

private fun isStaticTag(
    step: ResolveStep,
    resolvedCall: ResolvedCall<*>,
    context: ExpressionTypingContext
): Boolean {
    return when (step) {
        is ResolveStep.Root -> when (step.openExpr) {
            is KtQualifiedExpression -> {
                val parts = step.openExpr.asQualifierPartList()
                val targets = parts
                    .mapNotNull { it.expression }
                    .mapNotNull { context.trace[BindingContext.REFERENCE_TARGET, it] }

                if (parts.size != targets.size) return false

                val first = targets.first()

                if (!first.isRoot()) return false

                for (target in targets) {
                    val isValid = isValidStaticQualifiedPart(target)
                    if (!isValid)
                        return false
                }
                // TODO(lmr): is there more we need to do here?
                return true
            }
            is KtSimpleNameExpression -> {
                when (resolvedCall) {
                    is VariableAsFunctionResolvedCall -> {
                        val variableDescriptor = resolvedCall.variableCall.candidateDescriptor
                        if (variableDescriptor.isVar) return false
                        if (variableDescriptor.isConst) return true
                        val isRoot = variableDescriptor.isRoot()
                        when (variableDescriptor) {
                            is PropertyDescriptor ->
                                (variableDescriptor.getter?.isDefault ?: false) && isRoot
                            else -> false
                        }
                    }
                    else -> true
                }
            }
            else -> false
        }
        is ResolveStep.Nested -> step.isStaticCall
    }
}

private fun isValidStaticQualifiedPart(target: DeclarationDescriptor): Boolean {
    return when (target) {
        is ClassDescriptor -> when {
            target.kind == ClassKind.OBJECT -> true
            target.isCompanionObject -> true
            else -> false
        }
        is ClassConstructorDescriptor -> true
        is PropertyDescriptor -> when {
            target.isVar -> false
            target.isConst -> true
            target.getter?.isDefault == true -> true
            else -> false
        }
        is FieldDescriptor -> isValidStaticQualifiedPart(target.correspondingProperty)
        is SimpleFunctionDescriptor -> true
        else -> {
            false
        }
    }
}

private fun DeclarationDescriptor.isRoot() =
    containingDeclaration?.containingDeclaration is ModuleDescriptor

private fun BindingTrace.recordAttributeKeyRef(expr: KtExpression?, value: DeclarationDescriptor) {
    val key = expr as? KtReferenceExpression ?: error("expected a KtReferenceExpression")
    val prev = get(ComposeWritableSlices.ATTRIBUTE_KEY_REFERENCE_TARGET, key) ?: emptySet()
    record(ComposeWritableSlices.ATTRIBUTE_KEY_REFERENCE_TARGET, key, prev + value)
}

private fun BindingTrace.recordFailedCandidates(
    expr: KtElement,
    results: Collection<ResolvedCall<FunctionDescriptor>>?
) {
    if (results == null) return
    val prev = get(ComposeWritableSlices.FAILED_CANDIDATES, expr) ?: emptyList()
    record(ComposeWritableSlices.FAILED_CANDIDATES, expr, prev + results)
}

enum class ComposerCallKind { CALL, EMIT }

private val ResolvedCall<*>.semanticCall: ResolvedCall<*>
    get() = when (this) {
        is VariableAsFunctionResolvedCall -> variableCall
        else -> this
    }

private val Collection<ValueParameterDescriptor>.possibleChildrenParameter:
        ValueParameterDescriptor?
    get() = maxBy { it.index }?.let { if (it.type.isFunctionType) it else null }
