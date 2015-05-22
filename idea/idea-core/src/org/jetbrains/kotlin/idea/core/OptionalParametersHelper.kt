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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.copied
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import java.util.ArrayList
import java.util.HashMap

public object OptionalParametersHelper {
    public fun detectArgumentsToDropForDefaults(
            resolvedCall: ResolvedCall<out CallableDescriptor>,
            project: Project,
            canDrop: (ValueArgument) -> Boolean = { true }
    ): Collection<ValueArgument> {
        if (!resolvedCall.getStatus().isSuccess()) return emptyList()
        val descriptor = resolvedCall.getResultingDescriptor()

        val parameterToDefaultValue = descriptor.getValueParameters()
                .map { parameter -> defaultParameterValue(parameter, project)?.let { parameter to it } }
                .filterNotNull()
                .toMap()
        if (parameterToDefaultValue.isEmpty()) return emptyList()

        val arguments = resolvedCall.getCall().getValueArguments()
        val argumentsToDrop = ArrayList<ValueArgument>()

        //TODO: can drop arguments leaving last functional literal one (outside of parenthesis)

        for (argument in arguments.reverse()) {
            if (!canDrop(argument) || !argument.matchesDefault(resolvedCall, parameterToDefaultValue)) {
                if (!argument.isNamed()) break else continue // for a named argument we can try to drop arguments before it as well
            }

            argumentsToDrop.add(argument)
        }
        return argumentsToDrop
    }

    private fun ValueArgument.matchesDefault(resolvedCall: ResolvedCall<out CallableDescriptor>, parameterToDefaultValue: Map<ValueParameterDescriptor, DefaultValue>): Boolean {
        val parameter = resolvedCall.getParameterForArgument(this) ?: return false
        val defaultValue = parameterToDefaultValue[parameter] ?: return false
        val expression = defaultValue.substituteArguments(resolvedCall)
        val argumentExpression = getArgumentExpression()!!
        return argumentExpression.getText() == expression.getText() //TODO
    }

    private fun DefaultValue.substituteArguments(resolvedCall: ResolvedCall<out CallableDescriptor>): JetExpression {
        if (parameterUsages.isEmpty()) return expression

        val key = Key<JetExpression>("SUBSTITUTION")

        for ((parameter, usages) in parameterUsages) {
            val resolvedArgument = resolvedCall.getValueArguments()[parameter]!!
            if (resolvedArgument is ExpressionValueArgument) {
                val argument = resolvedArgument.getValueArgument()!!.getArgumentExpression()!!
                usages.forEach { it.putCopyableUserData(key, argument) }
            }
            //TODO: vararg
        }

        var expressionCopy = expression.copied()

        expression.forEachDescendantOfType<JetExpression> { it.putCopyableUserData(key, null) }

        val replacements = ArrayList<Pair<JetExpression, JetExpression>>()
        expressionCopy.forEachDescendantOfType<JetExpression> {
            val replacement = it.getCopyableUserData(key)
            if (replacement != null) {
                replacements.add(it to replacement)
            }
        }

        for ((expression, replacement) in replacements) {
            val replaced = expression.replace(replacement) as JetExpression
            if (expression == expressionCopy) {
                expressionCopy = replaced
            }
        }

        return expressionCopy
    }

    public data class DefaultValue(
            public val expression: JetExpression,
            public val parameterUsages: Map<ValueParameterDescriptor, Collection<JetExpression>>
    )

    //TODO: handle imports
    //TODO: handle implicit receivers
    public fun defaultParameterValue(parameter: ValueParameterDescriptor, project: Project): DefaultValue? {
        if (!parameter.hasDefaultValue()) return null
        //TODO: parameter in overriding method!
        val sourceParameter = DescriptorToSourceUtilsIde.getAnyDeclaration(project, parameter) as? JetParameter ?: return null
        //TODO: use JetSourceNavigationHelper
        val expression = sourceParameter.getDefaultValue() ?: return null

        val allParameters = (parameter.getContainingDeclaration() as CallableDescriptor).getValueParameters().toSet()

        val parameterUsages = HashMap<ValueParameterDescriptor, MutableCollection<JetExpression>>()

        val bindingContext = expression.analyze()
        expression.forEachDescendantOfType<JetSimpleNameExpression> {
            val target = bindingContext[BindingContext.REFERENCE_TARGET, it]
            if (target is ValueParameterDescriptor && target in allParameters) {
                parameterUsages.getOrPut(target) { ArrayList() }.add(it)
            }
        }

        return DefaultValue(expression, parameterUsages)
    }
}