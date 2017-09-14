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

package org.jetbrains.kotlin.idea.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.scopes.ExplicitImportsScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.addImportingScope
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.util.descriptorsEqualWithSubstitution
import java.util.*

class ShadowedDeclarationsFilter(
        private val bindingContext: BindingContext,
        private val resolutionFacade: ResolutionFacade,
        private val context: PsiElement,
        private val explicitReceiverValue: ReceiverValue?
) {
    companion object {
        fun create(
                bindingContext: BindingContext,
                resolutionFacade: ResolutionFacade,
                context: PsiElement,
                callTypeAndReceiver: CallTypeAndReceiver<*, *>
        ): ShadowedDeclarationsFilter? {
            val receiverExpression = when (callTypeAndReceiver) {
                is CallTypeAndReceiver.DEFAULT -> null
                is CallTypeAndReceiver.DOT -> callTypeAndReceiver.receiver
                is CallTypeAndReceiver.SAFE -> callTypeAndReceiver.receiver
                is CallTypeAndReceiver.SUPER_MEMBERS -> callTypeAndReceiver.receiver
                is CallTypeAndReceiver.INFIX -> callTypeAndReceiver.receiver
                is CallTypeAndReceiver.TYPE, is CallTypeAndReceiver.ANNOTATION -> null // need filtering of classes with the same FQ-name
                else -> return null // TODO: support shadowed declarations filtering for callable references
            }

            val explicitReceiverValue = receiverExpression?.let {
                val type = bindingContext.getType(it) ?: return null
                ExpressionReceiver.create(it, type, bindingContext)
            }
            return ShadowedDeclarationsFilter(bindingContext, resolutionFacade, context, explicitReceiverValue)
        }
    }

    private val psiFactory = KtPsiFactory(resolutionFacade.project)
    private val dummyExpressionFactory = DummyExpressionFactory(psiFactory)

    fun <TDescriptor : DeclarationDescriptor> filter(declarations: Collection<TDescriptor>): Collection<TDescriptor> {
        return declarations
                .groupBy { signature(it) }
                .values
                .flatMap { group -> filterEqualSignatureGroup(group) }
    }

    fun <TDescriptor : DeclarationDescriptor> createNonImportedDeclarationsFilter(
            importedDeclarations: Collection<DeclarationDescriptor>
    ): (Collection<TDescriptor>) -> Collection<TDescriptor> {
        val importedDeclarationsSet = importedDeclarations.toSet()
        val importedDeclarationsBySignature = importedDeclarationsSet.groupBy { signature(it) }

        return filter@ { declarations ->
            // optimization
            if (declarations.size == 1 && importedDeclarationsBySignature[signature(declarations.single())] == null) return@filter declarations

            val nonImportedDeclarations = declarations.filter { it !in importedDeclarationsSet }

            val notShadowed = HashSet<DeclarationDescriptor>()
            // same signature non-imported declarations from different packages do not shadow each other
            for ((pair, group) in nonImportedDeclarations.groupBy { signature(it) to packageName(it) }) {
                val imported = importedDeclarationsBySignature[pair.first]
                val all = if (imported != null) group + imported else group
                notShadowed.addAll(filterEqualSignatureGroup(all, descriptorsToImport = group))
            }
            declarations.filter { it in notShadowed }
        }
    }

    private fun signature(descriptor: DeclarationDescriptor): Any {
        return when (descriptor) {
            is SimpleFunctionDescriptor -> FunctionSignature(descriptor)
            is VariableDescriptor -> descriptor.name
            is ClassDescriptor -> descriptor.importableFqName ?: descriptor
            else -> descriptor
        }
    }

    private fun packageName(descriptor: DeclarationDescriptor) = descriptor.importableFqName?.parent()

    private fun <TDescriptor : DeclarationDescriptor> filterEqualSignatureGroup(
            descriptors: Collection<TDescriptor>,
            descriptorsToImport: Collection<TDescriptor> = emptyList()
    ): Collection<TDescriptor> {
        if (descriptors.size == 1) return descriptors

        val first = descriptors.first()

        if (first is ClassDescriptor) { // for classes with the same FQ-name we simply take the first one
            return listOf<TDescriptor>(first)
        }

        val isFunction = first is FunctionDescriptor
        val name = first.name
        val parameters = (first as CallableDescriptor).valueParameters

        val dummyArgumentExpressions = dummyExpressionFactory.createDummyExpressions(parameters.size)

        val bindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace for filtering shadowed declarations")
        for ((expression, parameter) in dummyArgumentExpressions.zip(parameters)) {
            bindingTrace.recordType(expression, parameter.varargElementType ?: parameter.type)
            bindingTrace.record(BindingContext.PROCESSED, expression, true)
        }

        val firstVarargIndex = parameters.withIndex().firstOrNull { it.value.varargElementType != null }?.index
        val useNamedFromIndex = if (firstVarargIndex != null && firstVarargIndex != parameters.lastIndex) firstVarargIndex else parameters.size

        class DummyArgument(val index: Int) : ValueArgument {
            private val expression = dummyArgumentExpressions[index]

            private val argumentName: ValueArgumentName? = if (isNamed()) {
                object : ValueArgumentName {
                    override val asName = parameters[index].name
                    override val referenceExpression = null
                }
            }
            else {
                null
            }

            override fun getArgumentExpression() = expression
            override fun isNamed() = index >= useNamedFromIndex
            override fun getArgumentName() = argumentName
            override fun asElement() = expression
            override fun getSpreadElement() = null
            override fun isExternal() = false
        }

        val arguments = ArrayList<DummyArgument>()
        for (i in parameters.indices) {
            arguments.add(DummyArgument(i))
        }

        val newCall = object : Call {
            //TODO: compiler crash (KT-8011)
            //val arguments = parameters.indices.map { DummyArgument(it) }
            val callee = psiFactory.createExpressionByPattern("$0", name, reformat = false)

            override fun getCalleeExpression() = callee

            override fun getValueArgumentList() = null

            override fun getValueArguments() = arguments

            override fun getFunctionLiteralArguments() = emptyList<LambdaArgument>()

            override fun getTypeArguments() = emptyList<KtTypeProjection>()

            override fun getTypeArgumentList() = null

            override fun getDispatchReceiver() = null

            override fun getCallOperationNode() = null

            override fun getExplicitReceiver() = explicitReceiverValue

            override fun getCallElement() = callee

            override fun getCallType() = Call.CallType.DEFAULT
        }

        var scope = context.getResolutionScope(bindingContext, resolutionFacade)

        if (descriptorsToImport.isNotEmpty()) {
            scope = scope.addImportingScope(ExplicitImportsScope(descriptorsToImport))
        }

        val dataFlowInfo = bindingContext.getDataFlowInfoBefore(context)
        val context = BasicCallResolutionContext.create(bindingTrace, scope, newCall, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo,
                                                        ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                                                        false, resolutionFacade.frontendService<LanguageVersionSettings>())
        val callResolver = resolutionFacade.frontendService<CallResolver>()
        val results = if (isFunction) callResolver.resolveFunctionCall(context) else callResolver.resolveSimpleProperty(context)
        val resultingDescriptors = results.resultingCalls.map { it.resultingDescriptor }
        val resultingOriginals = resultingDescriptors.mapTo(HashSet<DeclarationDescriptor>()) { it.original }
        val filtered = descriptors.filter { candidateDescriptor ->
            candidateDescriptor.original in resultingOriginals /* optimization */
            && resultingDescriptors.any { descriptorsEqualWithSubstitution(it, candidateDescriptor) }
        }
        return if (filtered.isNotEmpty()) filtered else descriptors /* something went wrong, none of our declarations among resolve candidates, let's not filter anything */
    }

    private class DummyExpressionFactory(val factory: KtPsiFactory) {
        private val expressions = ArrayList<KtExpression>()

        fun createDummyExpressions(count: Int): List<KtExpression> {
            while (expressions.size < count) {
                expressions.add(factory.createExpression("dummy"))
            }
            return expressions.take(count)
        }
    }

    private class FunctionSignature(val function: FunctionDescriptor) {
        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is FunctionSignature) return false
            if (function.name != other.function.name) return false
            val parameters1 = function.valueParameters
            val parameters2 = other.function.valueParameters
            if (parameters1.size != parameters2.size) return false
            for (i in parameters1.indices) {
                val p1 = parameters1[i]
                val p2 = parameters2[i]
                if (p1.varargElementType != p2.varargElementType) return false // both should be vararg or or both not
                if (p1.type != p2.type) return false
            }
            return true
        }

        override fun hashCode() = function.name.hashCode() * 17 + function.valueParameters.size
    }
}