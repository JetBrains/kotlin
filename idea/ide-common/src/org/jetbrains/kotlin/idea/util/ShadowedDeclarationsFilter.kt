/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.scopes.ExplicitImportsScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.util.descriptorsEqualWithSubstitution
import java.util.*

public class ShadowedDeclarationsFilter(
        private val bindingContext: BindingContext,
        private val resolutionFacade: ResolutionFacade,
        private val context: JetExpression,
        explicitReceiverData: ReferenceVariantsHelper.ExplicitReceiverData?
) {
    private val psiFactory = JetPsiFactory(resolutionFacade.project)
    private val dummyExpressionFactory = DummyExpressionFactory(psiFactory)

    private val explicitReceiverValue = explicitReceiverData?.let {
        val type = bindingContext.getType(it.expression) ?: return@let null
        ExpressionReceiver(it.expression, type)
    } ?: ReceiverValue.NO_RECEIVER

    public fun <TDescriptor : DeclarationDescriptor> filter(declarations: Collection<TDescriptor>): Collection<TDescriptor> {
        return declarations
                .groupBy { signature(it) }
                .values()
                .flatMap { group -> filterEqualSignatureGroup(group) }
    }

    public fun <TDescriptor : DeclarationDescriptor> filterNonImported(
            declarations: Collection<TDescriptor>,
            importedDeclarations: Collection<DeclarationDescriptor>
    ): Collection<TDescriptor> {
        val importedDeclarationsSet = importedDeclarations.toSet()
        val nonImportedDeclarations = declarations.filter { it !in importedDeclarationsSet }

        val importedDeclarationsBySignature = importedDeclarationsSet.groupBy { signature(it) }

        val notShadowed = HashSet<DeclarationDescriptor>()
        // same signature non-imported declarations from different packages do not shadow each other
        for ((pair, group) in nonImportedDeclarations.groupBy { signature(it) to packageName(it) }) {
            val imported = importedDeclarationsBySignature[pair.first]
            val all = if (imported != null) group + imported else group
            notShadowed.addAll(filterEqualSignatureGroup(all, descriptorsToImport = group))
        }
        return declarations.filter { it in notShadowed }
    }

    private fun signature(descriptor: DeclarationDescriptor): Any {
        return when (descriptor) {
            is SimpleFunctionDescriptor -> FunctionSignature(descriptor)
            is VariableDescriptor -> descriptor.getName()
            else -> descriptor
        }
    }

    private fun packageName(descriptor: DeclarationDescriptor) = descriptor.importableFqName?.parent()

    private fun <TDescriptor : DeclarationDescriptor> filterEqualSignatureGroup(
            descriptors: Collection<TDescriptor>,
            descriptorsToImport: Collection<TDescriptor> = emptyList()
    ): Collection<TDescriptor> {
        if (descriptors.size() == 1) return descriptors

        val first = descriptors.first()
        val isFunction = first is FunctionDescriptor
        val name = first.getName()
        val parameters = (first as CallableDescriptor).getValueParameters()

        val dummyArgumentExpressions = dummyExpressionFactory.createDummyExpressions(parameters.size())

        val bindingTrace = DelegatingBindingTrace(bindingContext, "Temporary trace for filtering shadowed declarations")
        for ((expression, parameter) in dummyArgumentExpressions.zip(parameters)) {
            bindingTrace.recordType(expression, parameter.getVarargElementType() ?: parameter.getType())
            bindingTrace.record(BindingContext.PROCESSED, expression, true)
        }

        val firstVarargIndex = parameters.withIndex().firstOrNull { it.value.getVarargElementType() != null }?.index
        val useNamedFromIndex = if (firstVarargIndex != null && firstVarargIndex != parameters.lastIndex) firstVarargIndex else parameters.size()

        class DummyArgument(val index: Int) : ValueArgument {
            private val expression = dummyArgumentExpressions[index]

            private val argumentName: ValueArgumentName? = if (isNamed()) {
                object : ValueArgumentName {
                    override val asName = parameters[index].getName()
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
            val callee = psiFactory.createExpressionByPattern("$0", name)

            override fun getCalleeExpression() = callee

            override fun getValueArgumentList() = null

            override fun getValueArguments() = arguments

            override fun getFunctionLiteralArguments() = emptyList<FunctionLiteralArgument>()

            override fun getTypeArguments() = emptyList<JetTypeProjection>()

            override fun getTypeArgumentList() = null

            override fun getDispatchReceiver() = ReceiverValue.NO_RECEIVER

            override fun getCallOperationNode() = null

            override fun getExplicitReceiver() = explicitReceiverValue

            override fun getCallElement() = callee

            override fun getCallType() = Call.CallType.DEFAULT
        }

        var lexicalScope = bindingContext[BindingContext.LEXICAL_SCOPE, context] ?: return descriptors

        if (descriptorsToImport.isNotEmpty()) {
            lexicalScope = lexicalScope.addImportScope(ExplicitImportsScope(descriptorsToImport))
        }

        val dataFlowInfo = bindingContext.getDataFlowInfo(context)
        val context = BasicCallResolutionContext.create(bindingTrace, lexicalScope, newCall, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo,
                                                        ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                                                        CallChecker.DoNothing, false)
        val callResolver = resolutionFacade.frontendService<CallResolver>()
        val results = if (isFunction) callResolver.resolveFunctionCall(context) else callResolver.resolveSimpleProperty(context)
        val resultingDescriptors = results.getResultingCalls().map { it.getResultingDescriptor() }
        val resultingOriginals = resultingDescriptors.mapTo(HashSet<DeclarationDescriptor>()) { it.getOriginal() }
        val filtered = descriptors.filter { candidateDescriptor ->
            candidateDescriptor.getOriginal() in resultingOriginals /* optimization */
                && resultingDescriptors.any { descriptorsEqualWithSubstitution(it, candidateDescriptor) }
        }
        return if (filtered.isNotEmpty()) filtered else descriptors /* something went wrong, none of our declarations among resolve candidates, let's not filter anything */
    }

    private class DummyExpressionFactory(val factory: JetPsiFactory) {
        private val expressions = ArrayList<JetExpression>()

        fun createDummyExpressions(count: Int): List<JetExpression> {
            while (expressions.size() < count) {
                expressions.add(factory.createExpression("dummy"))
            }
            return expressions.take(count)
        }
    }

    private class FunctionSignature(val function: FunctionDescriptor) {
        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is FunctionSignature) return false
            if (function.getName() != other.function.getName()) return false
            val parameters1 = function.getValueParameters()
            val parameters2 = other.function.getValueParameters()
            if (parameters1.size() != parameters2.size()) return false
            for (i in parameters1.indices) {
                val p1 = parameters1[i]
                val p2 = parameters2[i]
                if (p1.getVarargElementType() != p2.getVarargElementType()) return false // both should be vararg or or both not
                if (p1.getType() != p2.getType()) return false
            }
            return true
        }

        override fun hashCode() = function.getName().hashCode() * 17 + function.getValueParameters().size()
    }
}