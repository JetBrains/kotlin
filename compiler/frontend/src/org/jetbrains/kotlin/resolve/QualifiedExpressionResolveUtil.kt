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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.descriptorUtil.classObjectType
import org.jetbrains.kotlin.resolve.descriptorUtil.hasClassObjectType
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassifierQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.PackageQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext


public fun resolveAsReceiverInQualifiedExpression(
        qualifier: QualifierReceiver,
        context: ExpressionTypingContext,
        selector: DeclarationDescriptor?,
        symbolUsageValidator: SymbolUsageValidator
) {
    resolveAndRecordReferenceTarget(qualifier, context, selector, symbolUsageValidator)

    if (qualifier is ClassifierQualifier) {
        val classifier = qualifier.classifier
        if (classifier is TypeParameterDescriptor) {
            context.trace.report(Errors.TYPE_PARAMETER_ON_LHS_OF_DOT.on(qualifier.referenceExpression, classifier))
        }
        else if (classifier is ClassDescriptor && classifier.hasClassObjectType) {
            context.trace.recordType(qualifier.expression, classifier.classObjectType)
        }
    }
}

public fun resolveAsStandaloneExpression(
        qualifier: QualifierReceiver,
        context: ExpressionTypingContext,
        symbolUsageValidator: SymbolUsageValidator
) {
    resolveAndRecordReferenceTarget(qualifier, context, selector = null, symbolUsageValidator = symbolUsageValidator)

    if (qualifier is ClassifierQualifier) {
        val classifier = qualifier.classifier
        if (classifier is TypeParameterDescriptor) {
            context.trace.report(Errors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION.on(qualifier.referenceExpression, classifier))
        }
        else if (classifier is ClassDescriptor && !classifier.hasClassObjectType) {
            context.trace.report(Errors.NO_COMPANION_OBJECT.on(qualifier.referenceExpression, classifier))
        }
    }
    else if (qualifier is PackageQualifier) {
        context.trace.report(Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND.on(qualifier.referenceExpression))
    }
}

private fun resolveAndRecordReferenceTarget(
        qualifier: QualifierReceiver,
        context: ExpressionTypingContext,
        selector: DeclarationDescriptor?,
        symbolUsageValidator: SymbolUsageValidator
) {
    // TODO get rid of QualifierReceiver::resultingDescriptor. REFERENCE_TARGET should be enough.
    qualifier.resultingDescriptor = resolveReferenceTarget(qualifier, context, selector, symbolUsageValidator)
    context.trace.record(BindingContext.REFERENCE_TARGET, qualifier.referenceExpression, qualifier.resultingDescriptor)
}

private fun resolveReferenceTarget(
        qualifier: QualifierReceiver,
        context: ExpressionTypingContext,
        selector: DeclarationDescriptor?,
        symbolUsageValidator: SymbolUsageValidator
): DeclarationDescriptor {
    if (qualifier is ClassifierQualifier && qualifier.classifier is TypeParameterDescriptor) {
        return qualifier.classifier
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

    if (qualifier is ClassQualifier) {
        if (selector is CallableDescriptor &&
            (selector.dispatchReceiverParameter != null || selector.extensionReceiverParameter != null) &&
            qualifier.classifier is ClassDescriptor &&
            qualifier.classifier.hasClassObjectType
        ) {
            val companionObjectDescriptor = qualifier.classifier.companionObjectDescriptor
            if (companionObjectDescriptor != null) {
                context.trace.record(BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, qualifier.referenceExpression, qualifier.classifier)
                symbolUsageValidator.validateTypeUsage(companionObjectDescriptor, context.trace, qualifier.referenceExpression)
                return companionObjectDescriptor
            }
        }
    }

    return qualifier.descriptor
}
