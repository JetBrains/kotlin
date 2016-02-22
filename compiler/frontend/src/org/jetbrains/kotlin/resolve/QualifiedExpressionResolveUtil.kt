/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueTypeDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.hasCompanionObject
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.PackageQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.Qualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.TypeParameterQualifier
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext

fun resolveQualifierAsReceiverInExpression(
        qualifier: Qualifier,
        selector: DeclarationDescriptor?,
        context: ExpressionTypingContext,
        symbolUsageValidator: SymbolUsageValidator
): DeclarationDescriptor {
    val referenceTarget = resolveQualifierReferenceTarget(qualifier, selector, context, symbolUsageValidator)

    if (referenceTarget is TypeParameterDescriptor) {
        context.trace.report(Errors.TYPE_PARAMETER_ON_LHS_OF_DOT.on(qualifier.referenceExpression, referenceTarget))
    }

    return referenceTarget
}

fun resolveQualifierAsStandaloneExpression(
        qualifier: Qualifier,
        context: ExpressionTypingContext,
        symbolUsageValidator: SymbolUsageValidator
): DeclarationDescriptor {
    val referenceTarget = resolveQualifierReferenceTarget(qualifier, null, context, symbolUsageValidator)

    when (referenceTarget) {
        is TypeParameterDescriptor -> {
            context.trace.report(Errors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION.on(qualifier.referenceExpression, referenceTarget))
        }
        is ClassDescriptor -> {
            if (!referenceTarget.kind.isSingleton) {
                context.trace.report(Errors.NO_COMPANION_OBJECT.on(qualifier.referenceExpression, referenceTarget))
            }
        }
        is PackageViewDescriptor -> {
            context.trace.report(Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND.on(qualifier.referenceExpression))
        }
    }

    return referenceTarget
}

private fun resolveQualifierReferenceTarget(
        qualifier: Qualifier,
        selector: DeclarationDescriptor?,
        context: ExpressionTypingContext,
        symbolUsageValidator: SymbolUsageValidator
): DeclarationDescriptor {
    if (qualifier is TypeParameterQualifier) {
        return qualifier.descriptor
    }

    val selectorContainer = when (selector) {
        is ConstructorDescriptor ->
            selector.containingDeclaration.containingDeclaration
        else ->
            selector?.containingDeclaration
    }

    if (qualifier is PackageQualifier &&
        (selectorContainer is PackageFragmentDescriptor || selectorContainer is PackageViewDescriptor) &&
        DescriptorUtils.getFqName(qualifier.packageView) == DescriptorUtils.getFqName(selectorContainer)
    ) {
        return qualifier.packageView
    }

    // TODO make decisions about short reference to companion object somewhere else
    if (qualifier is ClassQualifier) {
        val classifier = qualifier.descriptor
        val selectorIsCallable = selector is CallableDescriptor &&
                                 (selector.dispatchReceiverParameter != null || selector.extensionReceiverParameter != null)
        // TODO simplify this code.
        // Given a class qualifier in expression position,
        // it should provide a proper REFERENCE_TARGET (with type),
        // and, in case of implicit companion object reference, SHORT_REFERENCE_TO_COMPANION_OBJECT.
        val classValueDescriptor = classifier.classValueDescriptor
        if (selectorIsCallable && classValueDescriptor != null) {
            val classValueTypeDescriptor = classifier.classValueTypeDescriptor!!
            context.trace.record(BindingContext.REFERENCE_TARGET, qualifier.referenceExpression, classValueDescriptor)
            context.trace.recordType(qualifier.expression, classValueTypeDescriptor.defaultType)
            if (classifier.hasCompanionObject) {
                context.trace.record(BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, qualifier.referenceExpression, classifier)
                symbolUsageValidator.validateTypeUsage(classValueDescriptor, context.trace, qualifier.referenceExpression)
            }
            return classValueTypeDescriptor
        }
    }

    return qualifier.descriptor
}
