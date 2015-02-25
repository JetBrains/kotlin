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

package org.jetbrains.kotlin.resolve.scopes.receivers

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName
import org.jetbrains.kotlin.name.Name
import java.util.ArrayList
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.descriptorUtil.getClassObjectReferenceTarget
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.recordScopeAndDataFlowInfo
import kotlin.properties.Delegates
import org.jetbrains.kotlin.resolve.descriptorUtil.classObjectDescriptor
import org.jetbrains.kotlin.resolve.scopes.*

public trait Qualifier: ReceiverValue {

    public val expression: JetExpression

    public val packageView: PackageViewDescriptor?

    public val classifier: ClassifierDescriptor?

    public val name: Name
        get() = classifier?.getName() ?: packageView!!.getName()

    // package, classifier or class object descriptor
    public val resultingDescriptor: DeclarationDescriptor

    public val scope: JetScope
}

class QualifierReceiver (
        val referenceExpression: JetSimpleNameExpression,
        override val packageView: PackageViewDescriptor?,
        override val classifier: ClassifierDescriptor?
) : Qualifier {

    override val expression: JetExpression = referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression

    val descriptor: DeclarationDescriptor
        get() = classifier ?: packageView ?: throw AssertionError("PackageView and classifier both are null")

    override var resultingDescriptor: DeclarationDescriptor by Delegates.notNull()

    override val scope: JetScope get() {
        val classObjectTypeScope = classifier?.getClassObjectType()?.getMemberScope()?.let {
            FilteringScope(it) { it !is ClassDescriptor }
        }
        val scopes = listOf(classObjectTypeScope, getNestedClassesAndPackageMembersScope()).filterNotNull().copyToArray()
        return ChainedScope(descriptor, "Member scope for " + name + " as package or class or object", *scopes)
    }

    fun getClassObjectReceiver(): ReceiverValue =
            classifier?.getClassObjectType()?.let { ExpressionReceiver(referenceExpression, it) } ?: ReceiverValue.NO_RECEIVER

    fun getNestedClassesAndPackageMembersScope(): JetScope {
        val scopes = ArrayList<JetScope>(4)

        scopes.addIfNotNull(packageView?.getMemberScope())

        if (classifier is ClassDescriptor) {
            scopes.add(classifier.getStaticScope())

            if (classifier.getKind() != ClassKind.ENUM_ENTRY) {
                scopes.add(DescriptorUtils.getStaticNestedClassesScope(classifier))
            }
        }

        return ChainedScope(descriptor, "Static scope for " + name + " as package or class or object", *scopes.copyToArray())
    }

    override fun getType(): JetType = throw IllegalStateException("No type corresponds to QualifierReceiver '$this'")

    override fun exists() = true

    override fun toString() = "Package{$packageView} OR Class{$classifier}"
}

fun createQualifier(
        expression: JetSimpleNameExpression,
        receiver: ReceiverValue,
        context: ExpressionTypingContext
): QualifierReceiver? {
    val receiverScope = when {
        !receiver.exists() -> context.scope
        receiver is QualifierReceiver -> receiver.scope
        else -> receiver.getType().getMemberScope()
    }

    val name = expression.getReferencedNameAsName()
    val packageViewDescriptor = receiverScope.getPackage(name)
    val classifierDescriptor = receiverScope.getClassifier(name)

    if (packageViewDescriptor == null && classifierDescriptor == null) return null

    context.recordScopeAndDataFlowInfo(expression)

    val qualifier = QualifierReceiver(expression, packageViewDescriptor, classifierDescriptor)
    context.trace.record(QUALIFIER, qualifier.expression, qualifier)
    return qualifier
}

private fun QualifierReceiver.resolveAsStandaloneExpression(context: ExpressionTypingContext): JetType? {
    resolveAndRecordReferenceTarget(context, selector = null)
    if (classifier is TypeParameterDescriptor) {
        context.trace.report(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION.on(referenceExpression, classifier))
    }
    else if (classifier is ClassDescriptor && classifier.getClassObjectType() == null) {
        context.trace.report(NO_CLASS_OBJECT.on(referenceExpression, classifier))
    }
    else if (packageView != null) {
        context.trace.report(EXPRESSION_EXPECTED_PACKAGE_FOUND.on(referenceExpression))
    }
    return null
}

private fun QualifierReceiver.resolveAsReceiverInQualifiedExpression(context: ExpressionTypingContext, selector: DeclarationDescriptor?) {
    resolveAndRecordReferenceTarget(context, selector)
    if (classifier is TypeParameterDescriptor) {
        context.trace.report(TYPE_PARAMETER_ON_LHS_OF_DOT.on(referenceExpression, classifier as TypeParameterDescriptor))
    }
    else if (classifier is ClassDescriptor && classifier.classObjectDescriptor != null) {
        context.trace.record(EXPRESSION_TYPE, expression, classifier.getClassObjectType())
    }
}

private fun QualifierReceiver.resolveAndRecordReferenceTarget(context: ExpressionTypingContext, selector: DeclarationDescriptor?) {
    resultingDescriptor = resolveReferenceTarget(context, selector)
    context.trace.record(REFERENCE_TARGET, referenceExpression, resultingDescriptor)
}

private fun QualifierReceiver.resolveReferenceTarget(
        context: ExpressionTypingContext,
        selector: DeclarationDescriptor?
): DeclarationDescriptor {
    if (classifier is TypeParameterDescriptor) {
        return classifier
    }

    val selectorContainer = when {
        selector is ConstructorDescriptor -> selector.getContainingDeclaration().getContainingDeclaration()
        else -> selector?.getContainingDeclaration()
    }

    if (packageView != null && (selectorContainer is PackageFragmentDescriptor || selectorContainer is PackageViewDescriptor)
            && getFqName(packageView) == getFqName(selectorContainer)) {
        return packageView
    }

    if (classifier is ClassDescriptor && classifier.classObjectDescriptor == selectorContainer) {
        if (classifier.getDefaultObjectDescriptor() != null) {
            context.trace.record(SHORT_REFERENCE_TO_DEFAULT_OBJECT, referenceExpression, classifier)
        }
        return classifier.getClassObjectReferenceTarget()
    }

    return descriptor
}
