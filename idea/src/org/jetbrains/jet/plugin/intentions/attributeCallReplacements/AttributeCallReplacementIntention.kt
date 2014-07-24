/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions.attributeCallReplacements

import org.jetbrains.jet.lang.psi.JetCallExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.plugin.intentions.JetSelfTargetingIntention
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.psi.ValueArgument
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.lang.resolve.calls.model.DefaultValueArgument
import org.jetbrains.jet.lang.resolve.calls.model.VarargValueArgument
import org.jetbrains.jet.plugin.util.Maybe
import org.jetbrains.jet.plugin.util.MaybeError
import org.jetbrains.jet.plugin.util.MaybeValue
import com.intellij.codeInsight.hint.HintManager
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getResolvedCall

// Internal because you shouldn't construct this manually. You can end up with an inconsistant CallDescription.
public class CallDescription internal (
        public val element: JetQualifiedExpression,
        public val callElement: JetCallExpression,
        public val resolved: ResolvedCall<out CallableDescriptor>
) {
    public val functionName: String?
        get() = callElement.getCalleeExpression()?.getText()

    public val argumentCount: Int
        get() = callElement.getValueArguments().size + callElement.getFunctionLiteralArguments().size

    public val hasTypeArguments: Boolean
        get() = callElement.getTypeArgumentList() != null

    public val hasEmptyArguments: Boolean
        get() = callElement.getValueArguments().any { it?.getArgumentExpression() == null }

    public fun getPositionalArguments(): Maybe<List<ValueArgument>, String> {

        val resolvedValueArguments = resolved.getValueArgumentsByIndex()
            ?: return MaybeError("duplicate.or.missing.arguments")

        // Check for mixed default and passed arguments and return the passed parameters (or fail)
        val indexOfFirstDefaultArgument = resolvedValueArguments.indexOf(DefaultValueArgument.DEFAULT)
        val valueArgumentGroups = if (indexOfFirstDefaultArgument >= 0) {
            if (resolvedValueArguments.listIterator(indexOfFirstDefaultArgument).any { it != DefaultValueArgument.DEFAULT }) {
                return MaybeError("skipped.defaults")
            }
            resolvedValueArguments.subList(0, indexOfFirstDefaultArgument)
        } else {
            resolvedValueArguments
        }

        // The vararg must be the last (passed) argument
        if (valueArgumentGroups.size > 0) {
            val vararg = valueArgumentGroups.find { it is VarargValueArgument }
            if (vararg != null && vararg != valueArgumentGroups.last) {
                return MaybeError("vararg.not.last")
            }
        }

        val valueArguments = valueArgumentGroups.flatMap { it.getArguments() }
        if (valueArguments.size < argumentCount) {
            // Only happens if invalid arguments were thrown away.
            return MaybeError("invalid.arguments")
        }

        return MaybeValue(valueArguments)
    }
}

public fun JetQualifiedExpression.toCallDescription(): CallDescription? {
    val callExpression = getSelectorExpression() as? JetCallExpression ?: return null

    val bindingContext = AnalyzerFacadeWithCache.getContextForElement(callExpression)
    // This should work. Nothing that returns a CallableDescriptor returns null and (out T is T)
    val resolvedCall = callExpression.getResolvedCall(bindingContext) ?:
        return null

    return CallDescription(this, callExpression, resolvedCall)
}

public abstract class AttributeCallReplacementIntention(name: String) : JetSelfTargetingIntention<JetDotQualifiedExpression>(name, javaClass()) {

    protected abstract fun isApplicableToCall(call: CallDescription): Boolean

    protected abstract fun replaceCall(call: CallDescription, editor: Editor): Unit
    protected open fun formatArgumentsFor(call: CallDescription): Array<Any?> = array()

    final override fun isApplicableTo(element: JetDotQualifiedExpression): Boolean {
        val callDescription = element.toCallDescription()
        if (callDescription != null && isApplicableToCall(callDescription)) {
            setText(JetBundle.message(key, *formatArgumentsFor(callDescription)))
            return true
        } else {
            return false
        }
    }

    final override fun applyTo(element: JetDotQualifiedExpression, editor: Editor) {
        // If this doesn't work, something is very wrong (isApplicableTo failed).
        replaceCall(element.toCallDescription()!!, editor)
    }

    protected open fun intentionFailed(editor: Editor, messageId: String, vararg values: Any?) {
        val message = "Intention failed: ${JetBundle.message("replace.call.error.${messageId}", *values)}"
        HintManager.getInstance().showErrorHint(editor, message)
    }

    protected fun handleErrors<V: Any>(editor: Editor, maybeValue: Maybe<V, String>): V? {
        return when (maybeValue) {
            is MaybeValue -> maybeValue.value
            is MaybeError -> {
                intentionFailed(editor, maybeValue.error)
                null
            }
            else -> throw NoWhenBranchMatchedException()
        }
    }
}
