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

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.di.InjectorForMacros
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CompositeChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckValueArgumentsMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.ExplicitImportsScope
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.types.TypeUtils
import java.util.ArrayList

public class ShadowedDeclarationsFilter(
        private val bindingContext: BindingContext,
        private val moduleDescriptor: ModuleDescriptor,
        private val project: Project,
        private val importDeclarations: Boolean = false
) {
    private val psiFactory = JetPsiFactory(project)
    private val dummyExpressionFactory = DummyExpressionFactory(psiFactory)

    public fun <TDescriptor : DeclarationDescriptor> filter(declarations: Collection<TDescriptor>, expression: JetSimpleNameExpression): Collection<TDescriptor> {
        val call = expression.getCall(bindingContext) ?: return declarations

        return declarations
                .groupBy { signature(it) }
                .flatMap { filterEqualSignatureGroup(it.value, call) }
    }

    private fun signature(descriptor: DeclarationDescriptor): Any {
        return when (descriptor) {
            is SimpleFunctionDescriptor -> FunctionSignature(descriptor)
            is VariableDescriptor -> descriptor.getName()
            else -> descriptor
        }
    }

    private fun <TDescriptor : DeclarationDescriptor> filterEqualSignatureGroup(descriptors: Collection<TDescriptor>, call: Call): Collection<TDescriptor> {
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

        val newCall = object : DelegatingCall(call) {
            //TODO: compiler crash (KT-8011)
            //val arguments = parameters.indices.map { DummyArgument(it) }
            val callee = psiFactory.createExpressionByPattern("$0", name)

            override fun getCalleeExpression() = callee

            override fun getValueArgumentList() = null

            override fun getValueArguments() = arguments

            override fun getFunctionLiteralArguments() = emptyList<FunctionLiteralArgument>()

            override fun getTypeArguments() = emptyList<JetTypeProjection>()

            override fun getTypeArgumentList() = null
        }

        val calleeExpression = call.getCalleeExpression() ?: return descriptors
        var resolutionScope = bindingContext.correctedResolutionScope(calleeExpression) ?: return descriptors

        if (importDeclarations) {
            val importableDescriptors = descriptors.filter { it.canBeReferencedViaImport() }
            resolutionScope = ChainedScope(resolutionScope.getContainingDeclaration(), "Scope with explicitly imported descriptors",
                                           ExplicitImportsScope(importableDescriptors), resolutionScope)
        }

        val dataFlowInfo = bindingContext.getDataFlowInfo(calleeExpression)
        val context = BasicCallResolutionContext.create(bindingTrace, resolutionScope, newCall, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo,
                                                        ContextDependency.INDEPENDENT, CheckValueArgumentsMode.ENABLED,
                                                        CompositeChecker(listOf()), SymbolUsageValidator.Empty, AdditionalTypeChecker.Composite(listOf()), false)
        val callResolver = InjectorForMacros(project, moduleDescriptor).getCallResolver()
        val results = if (isFunction) callResolver.resolveFunctionCall(context) else callResolver.resolveSimpleProperty(context)
        val resultingDescriptors = results.getResultingCalls().map { it.getResultingDescriptor() }.toSet()
        val filtered = descriptors.filter { it in resultingDescriptors }
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