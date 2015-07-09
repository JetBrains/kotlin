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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.synthetic.SyntheticExtensionPropertyDescriptor

class UsePropertyAccessSyntaxInspection : IntentionBasedInspection<JetCallExpression>(UsePropertyAccessSyntaxIntention())

class UsePropertyAccessSyntaxIntention : JetSelfTargetingOffsetIndependentIntention<JetCallExpression>(javaClass(), "Use property access syntax") {
    override fun isApplicableTo(element: JetCallExpression): Boolean {
        if (element.getQualifiedExpressionForSelector()?.getReceiverExpression() is JetSuperExpression) return false // cannot call extensions on "super"

        return findExtensionPropertyToUse(element) != null
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val propertyName = findExtensionPropertyToUse(element)!!.getName()
        val arguments = element.getValueArguments()
        when (arguments.size()) {
            0 -> replaceWithPropertyGet(element, propertyName)
            1 -> replaceWithPropertySet(element, propertyName, arguments.single())
            else -> error("More than one argument in call to accessor")
        }
    }

    private fun findExtensionPropertyToUse(callExpression: JetCallExpression): PropertyDescriptor? {
        val callee = callExpression.getCalleeExpression() as? JetSimpleNameExpression ?: return null

        val bindingContext = callExpression.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return null
        if (!resolvedCall.getStatus().isSuccess()) return null

        val function = resolvedCall.getResultingDescriptor() as? FunctionDescriptor ?: return null
        val resolutionScope = callExpression.getResolutionScope(bindingContext, callExpression.getResolutionFacade())
        val property = findSyntheticProperty(function, resolutionScope) ?: return null

        val moduleDescriptor = callExpression.getResolutionFacade().findModuleDescriptor(callExpression)
        val inDescriptor = resolutionScope.getContainingDeclaration()

        fun isVisible(descriptor: DeclarationDescriptor): Boolean {
            return if (descriptor is DeclarationDescriptorWithVisibility)
                descriptor.isVisible(inDescriptor, bindingContext, callee)
            else
                true
        }

        val referenceVariantsHelper = ReferenceVariantsHelper(bindingContext, moduleDescriptor, callExpression.getProject(), ::isVisible)
        val propertyName = property.getName()
        val accessibleVariables = referenceVariantsHelper.getReferenceVariants(callee, DescriptorKindFilter.VARIABLES, false, { it == propertyName })
        if (property !in accessibleVariables) return null // shadowed by something else

        return property
    }

    private fun findSyntheticProperty(function: FunctionDescriptor, resolutionScope: JetScope): SyntheticExtensionPropertyDescriptor? {
        SyntheticExtensionPropertyDescriptor.findByGetterOrSetter(function, resolutionScope)?.let { return it }

        for (overridden in function.getOverriddenDescriptors()) {
            findSyntheticProperty(overridden, resolutionScope)?.let { return it }
        }

        return null
    }

    private fun replaceWithPropertyGet(callExpression: JetCallExpression, propertyName: Name) {
        val newExpression = JetPsiFactory(callExpression).createExpression(propertyName.render())
        callExpression.replace(newExpression)
    }

    //TODO: what if it was used as expression (of type Unit)?
    private fun replaceWithPropertySet(callExpression: JetCallExpression, propertyName: Name, argument: JetValueArgument) {
        val qualifiedExpression = callExpression.getQualifiedExpressionForSelector()
        if (qualifiedExpression != null) {
            val pattern = when (qualifiedExpression) {
                is JetDotQualifiedExpression -> "$0.$1=$2"
                is JetSafeQualifiedExpression -> "$0?.$1=$2"
                else -> error(qualifiedExpression) //TODO: make it sealed?
            }
            val newExpression = JetPsiFactory(callExpression).createExpressionByPattern(
                    pattern,
                    qualifiedExpression.getReceiverExpression(),
                    propertyName,
                    argument.getArgumentExpression()!!
            )
            qualifiedExpression.replace(newExpression)
        }
        else {
            val newExpression = JetPsiFactory(callExpression).createExpressionByPattern("$0=$1", propertyName, argument.getArgumentExpression()!!)
            callExpression.replace(newExpression)
        }
    }
}