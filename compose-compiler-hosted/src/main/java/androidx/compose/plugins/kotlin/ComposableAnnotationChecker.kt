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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtxElement
import androidx.compose.plugins.kotlin.analysis.ComponentMetadata
import androidx.compose.plugins.kotlin.analysis.ComposeDefaultErrorMessages
import androidx.compose.plugins.kotlin.analysis.ComposeErrors
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices.COMPOSABLE_ANALYSIS
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices.FCS_RESOLVEDCALL_COMPOSABLE
import androidx.compose.plugins.kotlin.analysis.ComposeWritableSlices.INFERRED_COMPOSABLE_DESCRIPTOR
import org.jetbrains.kotlin.resolve.AdditionalAnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.context.CallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlinedArgument
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.kotlin.util.OperatorNameConventions

open class ComposableAnnotationChecker(
    val nullableMode: Mode? = null
) : CallChecker, DeclarationChecker,
    AdditionalTypeChecker, AdditionalAnnotationChecker, StorageComponentContainerContributor {

    enum class Mode {
        /** @Composable annotations are enforced when inferred/specified **/
        KTX_CHECKED,
        /** @Composable annotations are explicitly required on all composables **/
        KTX_STRICT,
        /** @Composable annotations are explicitly required and are not subtypes of their unannotated types **/
        KTX_PEDANTIC,
        /** @Composable annotations are explicitly required and are not subtypes, but use FCS **/
        FCS
    }

    companion object {
        val DEFAULT_MODE =
            Mode.FCS

        fun get(project: Project): ComposableAnnotationChecker {
            return StorageComponentContainerContributor.getInstances(project).single {
                it is ComposableAnnotationChecker
            } as ComposableAnnotationChecker
        }
    }

    enum class Composability { NOT_COMPOSABLE, INFERRED, MARKED }

    fun shouldInvokeAsTag(trace: BindingTrace, resolvedCall: ResolvedCall<*>): Boolean {
        if (resolvedCall is VariableAsFunctionResolvedCall) {
            if (resolvedCall.variableCall.candidateDescriptor.type.hasComposableAnnotation())
                return true
            if (resolvedCall.variableCall.candidateDescriptor.isComposableFromChildrenAnnotation())
                return true
            if (resolvedCall.functionCall.resultingDescriptor.hasComposableAnnotation()) return true
            return false
        }
        val candidateDescriptor = resolvedCall.candidateDescriptor
        if (candidateDescriptor is FunctionDescriptor) {
            if (candidateDescriptor.isOperator &&
                candidateDescriptor.name == OperatorNameConventions.INVOKE) {
                if (resolvedCall.dispatchReceiver?.type?.hasComposableAnnotation() == true) {
                    return true
                }
            }
        }
        if (candidateDescriptor is FunctionDescriptor) {
            when (analyze(trace, candidateDescriptor)) {
                Composability.NOT_COMPOSABLE -> return false
                Composability.INFERRED -> return true
                Composability.MARKED -> return true
            }
        }
        if (candidateDescriptor is ValueParameterDescriptor) {
            if (candidateDescriptor.isComposableFromChildrenAnnotation()) return true
            return candidateDescriptor.type.hasComposableAnnotation()
        }
        if (candidateDescriptor is LocalVariableDescriptor) {
            return candidateDescriptor.type.hasComposableAnnotation()
        }
        if (candidateDescriptor is PropertyDescriptor) {
            if (candidateDescriptor.isComposableFromChildrenAnnotation()) return true
            return candidateDescriptor.type.hasComposableAnnotation()
        }
        return candidateDescriptor.hasComposableAnnotation()
    }

    open fun getMode(psi: PsiElement): Mode {
        if (nullableMode != null) return nullableMode
        return DEFAULT_MODE
    }

    fun analyze(trace: BindingTrace, descriptor: FunctionDescriptor): Composability {
        val unwrappedDescriptor =
            if (descriptor is
                        ComposeCallResolutionInterceptorExtension.ComposableInvocationDescriptor)
                descriptor.original
            else descriptor
        val psi = unwrappedDescriptor.findPsi() as? KtElement
        psi?.let { trace.bindingContext.get(COMPOSABLE_ANALYSIS, it)?.let { return it } }
        if (unwrappedDescriptor.name == Name.identifier("compose") &&
            unwrappedDescriptor.containingDeclaration is ClassDescriptor &&
            ComponentMetadata.isComposeComponent(unwrappedDescriptor.containingDeclaration)
        ) return Composability.MARKED
        var composability = Composability.NOT_COMPOSABLE
        if (trace.bindingContext.get(
                INFERRED_COMPOSABLE_DESCRIPTOR,
                unwrappedDescriptor
            ) ?: false) {
            composability = Composability.MARKED
        } else {
            when (unwrappedDescriptor) {
                is VariableDescriptor ->
                    if (unwrappedDescriptor.hasComposableAnnotation() ||
                        unwrappedDescriptor.type.hasComposableAnnotation()
                    )
                        composability =
                            Composability.MARKED
                is ConstructorDescriptor ->
                    if (unwrappedDescriptor.hasComposableAnnotation()) composability =
                        Composability.MARKED
                is JavaMethodDescriptor ->
                    if (unwrappedDescriptor.hasComposableAnnotation()) composability =
                        Composability.MARKED
                else -> if (unwrappedDescriptor.hasComposableAnnotation()) composability =
                    Composability.MARKED
            }
        }
        (unwrappedDescriptor.findPsi() as? KtElement)?.let {
                element -> composability = analyzeFunctionContents(trace, element, composability)
        }
        psi?.let { trace.record(COMPOSABLE_ANALYSIS, it, composability) }
        return composability
    }

    private fun analyzeFunctionContents(
        trace: BindingTrace,
        element: KtElement,
        signatureComposability: Composability
    ): Composability {
        var composability = signatureComposability
        val mode = getMode(element)
        var localKtx = false
        var localFcs = false
        var isInlineLambda = false
        element.accept(object : KtTreeVisitorVoid() {
            override fun visitKtxElement(element: KtxElement) {
                localKtx = true
            }

            override fun visitDeclaration(dcl: KtDeclaration) {
                if (dcl == element) {
                    super.visitDeclaration(dcl)
                }
            }

            override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                val isInlineable = isInlinedArgument(
                    lambdaExpression.functionLiteral,
                    trace.bindingContext,
                    true
                )
                if (isInlineable && lambdaExpression == element) isInlineLambda = true
                if (isInlineable || lambdaExpression == element)
                    super.visitLambdaExpression(lambdaExpression)
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                if (mode == Mode.FCS) {
                    // Can be null in cases where the call isn't resolvable (eg. due to a bug/typo in the user's code)
                    val isCallComposable = trace.get(FCS_RESOLVEDCALL_COMPOSABLE, expression)
                    if (isCallComposable == true) {
                        localFcs = true
                        if (!isInlineLambda && composability != Composability.MARKED) {
                            // Report error on composable element to make it obvious which invocation is offensive
                            val reportElement = expression.calleeExpression ?: expression
                            trace.reportFromPlugin(
                                ComposeErrors.COMPOSABLE_INVOCATION_IN_NON_COMPOSABLE
                                    .on(reportElement),
                                ComposeDefaultErrorMessages
                            )
                        }
                    }
                }
                super.visitCallExpression(expression)
            }
        }, null)
        if (
            (localKtx || localFcs) &&
            !isInlineLambda && composability != Composability.MARKED &&
            (mode == Mode.KTX_STRICT || mode == Mode.KTX_PEDANTIC || mode == Mode.FCS)
        ) {
            val reportElement = when (element) {
                is KtNamedFunction -> element.nameIdentifier ?: element
                else -> element
            }
            if (localFcs) {
                trace.reportFromPlugin(
                    ComposeErrors.COMPOSABLE_INVOCATION_IN_NON_COMPOSABLE.on(reportElement),
                    ComposeDefaultErrorMessages
                )
            }
            if (localKtx) {
                trace.reportFromPlugin(
                    ComposeErrors.KTX_IN_NON_COMPOSABLE.on(reportElement),
                    ComposeDefaultErrorMessages
                )
            }
        }
        if (localKtx && composability == Composability.NOT_COMPOSABLE)
            composability =
                Composability.INFERRED
        return composability
    }

    /**
     * Analyze a KtElement
     *  - Determine if it is @Compoasble (eg. the element or inferred type has an @Composable annotation)
     *  - Update the binding context to cache analysis results
     *  - Report errors (eg. KTX tags occur in a non-composable, invocations of an @Composable, etc)
     *  - Return true if element is @Composable, else false
     */
    fun analyze(trace: BindingTrace, element: KtElement, type: KotlinType?): Composability {
        trace.bindingContext.get(COMPOSABLE_ANALYSIS, element)?.let { return it }

        var composability =
            Composability.NOT_COMPOSABLE

        if (element is KtClass) {
            val descriptor = trace.bindingContext.get(BindingContext.CLASS, element)
                ?: error("Element class context not found")
            val annotationEntry = element.annotationEntries.singleOrNull {
                trace.bindingContext.get(BindingContext.ANNOTATION, it)?.isComposableAnnotation
                    ?: false
            }
            if (annotationEntry != null && !ComponentMetadata.isComposeComponent(descriptor)) {
                trace.report(
                    Errors.WRONG_ANNOTATION_TARGET.on(
                        annotationEntry,
                        "class which does not extend androidx.compose.Component"
                    )
                )
            }
            if (ComponentMetadata.isComposeComponent(descriptor)) {
                composability += Composability.MARKED
            }
        }
        if (element is KtParameter) {
            val childrenAnnotation = element.annotationEntries
                .mapNotNull { trace.bindingContext.get(BindingContext.ANNOTATION, it) }
                .singleOrNull { it.isComposableChildrenAnnotation }

            if (childrenAnnotation != null) {
                composability += Composability.MARKED
            }

            val composableAnnotation = element
                .typeReference
                ?.annotationEntries
                ?.mapNotNull { trace.bindingContext.get(BindingContext.ANNOTATION, it) }
                ?.singleOrNull { it.isComposableAnnotation }

            if (composableAnnotation != null) {
                composability += Composability.MARKED
            }
        }
        if (element is KtParameter) {
            val childrenAnnotation = element.annotationEntries
                .mapNotNull { trace.bindingContext.get(BindingContext.ANNOTATION, it) }
                .singleOrNull { it.isComposableChildrenAnnotation }

            if (childrenAnnotation != null) {
                composability += Composability.MARKED
            }

            val composableAnnotation = element
                .typeReference
                ?.annotationEntries
                ?.mapNotNull { trace.bindingContext.get(BindingContext.ANNOTATION, it) }
                ?.singleOrNull { it.isComposableAnnotation }

            if (composableAnnotation != null) {
                composability += Composability.MARKED
            }
        }

        // if (candidateDescriptor.type.arguments.size != 1 || !candidateDescriptor.type.arguments[0].type.isUnit()) return false
        if (
            type != null &&
            type !== TypeUtils.NO_EXPECTED_TYPE &&
            type.hasComposableAnnotation()
        ) {
            composability += Composability.MARKED
        }
        val parent = element.parent
        val annotations = when {
            element is KtNamedFunction -> element.annotationEntries
            parent is KtAnnotatedExpression -> parent.annotationEntries
            element is KtProperty -> element.annotationEntries
            element is KtParameter -> element.typeReference?.annotationEntries ?: emptyList()
            else -> emptyList()
        }

        for (entry in annotations) {
            val descriptor = trace.bindingContext.get(BindingContext.ANNOTATION, entry) ?: continue
            if (descriptor.isComposableAnnotation) {
                composability += Composability.MARKED
            }
            if (descriptor.isComposableChildrenAnnotation) {
                composability += Composability.MARKED
            }
        }

        if (element is KtLambdaExpression || element is KtFunction) {
            composability = analyzeFunctionContents(trace, element, composability)
        }

        trace.record(COMPOSABLE_ANALYSIS, element, composability)
        return composability
    }

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (platform != JvmPlatform) return
        container.useInstance(this)
    }

    override fun check(
        declaration: KtDeclaration,
        descriptor: DeclarationDescriptor,
        context: DeclarationCheckerContext
    ) {
        when (descriptor) {
            is ClassDescriptor -> {
                val trace = context.trace
                val element = descriptor.findPsi()
                if (element is KtClass) {
                    val classDescriptor =
                        trace.bindingContext.get(
                            BindingContext.CLASS,
                            element
                        ) ?: error("Element class context not found")
                    val composableAnnotationEntry = element.annotationEntries.singleOrNull {
                        trace.bindingContext.get(
                            BindingContext.ANNOTATION,
                            it
                        )?.isComposableAnnotation ?: false
                    }
                    if (composableAnnotationEntry != null &&
                        !ComponentMetadata.isComposeComponent(classDescriptor)) {
                        trace.report(
                            Errors.WRONG_ANNOTATION_TARGET.on(
                                composableAnnotationEntry,
                                "class which does not extend androidx.compose.Component"
                            )
                        )
                    }
                }
            }
            is PropertyDescriptor -> {}
            is LocalVariableDescriptor -> {}
            is TypeAliasDescriptor -> {}
            is FunctionDescriptor -> analyze(context.trace, descriptor)
            else ->
                throw Error("currently unsupported " + descriptor.javaClass)
        }
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val mode = getMode(reportOn)
        val shouldBeTag = shouldInvokeAsTag(context.trace, resolvedCall)
        if (mode == Mode.FCS) {
            context.trace.record(
                FCS_RESOLVEDCALL_COMPOSABLE,
                resolvedCall.call.callElement,
                shouldBeTag
            )
        }
        // NOTE: we return early here because the KtxCallResolver does its own composable checking (by delegating to this class) and has
        // more context to make the right call.
        if (reportOn.parent is KtxElement) return
        if (
            mode != Mode.FCS && context.resolutionContext is CallResolutionContext &&
            resolvedCall is ResolvedCallImpl &&
            (context.resolutionContext as CallResolutionContext).call.callElement is
                    KtCallExpression
        ) {
            if (shouldBeTag) {
                context.trace.reportFromPlugin(
                    ComposeErrors.SVC_INVOCATION.on(
                        reportOn as KtElement,
                        resolvedCall.candidateDescriptor.name.asString()
                    ), ComposeDefaultErrorMessages
                )
            }
        }
    }

    override fun checkType(
        expression: KtExpression,
        expressionType: KotlinType,
        expressionTypeWithSmartCast: KotlinType,
        c: ResolutionContext<*>
    ) {
        val mode = getMode(expression)
        if (mode != Mode.KTX_PEDANTIC && mode != Mode.FCS) return
        if (expression is KtLambdaExpression) {
            val expectedType = c.expectedType
            if (expectedType === TypeUtils.NO_EXPECTED_TYPE) return
            val expectedComposable = expectedType.hasComposableAnnotation()
            val composability = analyze(c.trace, expression, c.expectedType)
            if ((expectedComposable && composability == Composability.NOT_COMPOSABLE) ||
                (!expectedComposable && composability == Composability.MARKED)) {
                val isInlineable =
                    isInlinedArgument(
                        expression.functionLiteral,
                        c.trace.bindingContext,
                        true
                    )
                if (isInlineable) return

                val reportOn =
                    if (expression.parent is KtAnnotatedExpression)
                        expression.parent as KtExpression
                    else expression
                c.trace.report(
                    Errors.TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.on(
                        reportOn,
                        expectedType,
                        expressionTypeWithSmartCast
                    )
                )
            }
            return
        } else {
            val expectedType = c.expectedType

            if (expectedType === TypeUtils.NO_EXPECTED_TYPE) return
            if (expectedType === TypeUtils.UNIT_EXPECTED_TYPE) return

            val nullableAnyType = expectedType.builtIns.nullableAnyType
            val anyType = expectedType.builtIns.anyType

            if (anyType == expectedType.lowerIfFlexible() &&
                nullableAnyType == expectedType.upperIfFlexible()) return

            val expectedComposable = expectedType.hasComposableAnnotation()
            val isComposable = expressionType.hasComposableAnnotation()

            if (expectedComposable != isComposable) {
                val reportOn =
                    if (expression.parent is KtAnnotatedExpression)
                        expression.parent as KtExpression
                    else expression
                c.trace.report(
                    Errors.TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.on(
                        reportOn,
                        expectedType,
                        expressionTypeWithSmartCast
                    )
                )
            }
            return
        }
    }

    override fun checkEntries(
        entries: List<KtAnnotationEntry>,
        actualTargets: List<KotlinTarget>,
        trace: BindingTrace
    ) {
        val entry = entries.singleOrNull {
            trace.bindingContext.get(BindingContext.ANNOTATION, it)?.isComposableAnnotation ?: false
        }
        if ((entry?.parent as? KtAnnotatedExpression)?.baseExpression is
                    KtObjectLiteralExpression) {
            trace.report(
                Errors.WRONG_ANNOTATION_TARGET.on(
                    entry,
                    "class which does not extend androidx.compose.Component"
                )
            )
        }
    }

    operator fun Composability.plus(rhs: Composability): Composability =
        if (this > rhs) this else rhs
}
