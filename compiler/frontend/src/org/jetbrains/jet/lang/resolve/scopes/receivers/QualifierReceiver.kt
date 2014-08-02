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

package org.jetbrains.jet.lang.resolve.scopes.receivers

import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.DescriptorUtils.getFqName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope
import java.util.ArrayList
import org.jetbrains.jet.utils.addIfNotNull
import org.jetbrains.jet.lang.resolve.BindingContext.*
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.diagnostics.Errors.*
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext
import org.jetbrains.jet.lang.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector

public class QualifierReceiver(
        val expression: JetSimpleNameExpression,
        val packageView: PackageViewDescriptor?,
        val classifier: ClassifierDescriptor?
) : ReceiverValue {

    val name: Name
        get() = classifier?.getName() ?: packageView!!.getName()

    fun getClassObjectReceiver(): ReceiverValue =
            classifier?.getClassObjectType()?.let { ExpressionReceiver(expression, it) } ?: ReceiverValue.NO_RECEIVER

    val descriptor: DeclarationDescriptor
        get() = classifier ?: packageView ?: throw AssertionError("PackageView and classifier both are null")

    fun getScope(): JetScope {
        val scopes = listOf(classifier?.getClassObjectType()?.getMemberScope(), getNestedClassesAndPackageMembersScope()).filterNotNull().copyToArray()
        return ChainedScope(descriptor, "Member scope for " + name + " as package or class or object", *scopes as Array<JetScope?>)
    }

    fun getNestedClassesAndPackageMembersScope(): JetScope {
        val scopes = ArrayList<JetScope>(3)

        scopes.addIfNotNull(classifier?.getClassObjectType()?.getMemberScope())

        scopes.addIfNotNull(packageView?.getMemberScope())

        if (classifier is ClassDescriptor && classifier.getKind() != ClassKind.ENUM_ENTRY) {
            scopes.add(DescriptorUtils.getStaticNestedClassesScope(classifier))
        }

        return ChainedScope(descriptor, "Static scope for " + name + " as package or class or object", *scopes.copyToArray())
    }

    override fun getType(): JetType = throw IllegalStateException("No type corresponds to QualifierReceiver '$this'")

    override fun exists() = true

    override fun toString() = "Package{$packageView} OR Class{$classifier}"
}

fun createQualifierReceiver(
        expression: JetSimpleNameExpression,
        receiver: ReceiverValue,
        context: ExpressionTypingContext
): QualifierReceiver? {
    val receiverScope = when {
        !receiver.exists() -> context.scope
        receiver is QualifierReceiver -> receiver.getScope()
        else -> receiver.getType().getMemberScope()
    }

    val name = expression.getReferencedNameAsName()
    val packageViewDescriptor = receiverScope.getPackage(name)
    val classifierDescriptor = receiverScope.getClassifier(name)

    if (packageViewDescriptor == null && classifierDescriptor == null) return null

    context.trace.record(RESOLUTION_SCOPE, expression, context.scope)
    if (context.dataFlowInfo.hasTypeInfoConstraints()) {
        context.trace.record(NON_DEFAULT_EXPRESSION_DATA_FLOW, expression, context.dataFlowInfo)
    }

    val qualifierReceiver = QualifierReceiver(expression, packageViewDescriptor, classifierDescriptor)
    context.trace.record(QUALIFIER_RECEIVER, expression.getTopmostParentQualifiedExpressionForSelector() ?: expression, qualifierReceiver)
    return qualifierReceiver
}

private fun QualifierReceiver.resolveAsStandaloneExpression(context: ExpressionTypingContext): JetType? {
    context.trace.record(REFERENCE_TARGET, expression, resolveReferenceTarget(selector = null))
    if (classifier is TypeParameterDescriptor) {
        context.trace.report(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION.on(expression, classifier))
    }
    else if (classifier is ClassDescriptor && classifier.getClassObjectType() == null) {
        context.trace.report(NO_CLASS_OBJECT.on(expression, classifier))
    }
    else if (packageView != null) {
        context.trace.report(EXPRESSION_EXPECTED_PACKAGE_FOUND.on(expression))
    }
    return null
}

private fun QualifierReceiver.resolveAsReceiverInQualifiedExpression(context: ExpressionTypingContext, selector: DeclarationDescriptor?) {
    context.trace.record(REFERENCE_TARGET, expression, resolveReferenceTarget(selector))
    if (classifier is TypeParameterDescriptor) {
        context.trace.report(TYPE_PARAMETER_ON_LHS_OF_DOT.on(expression, classifier as TypeParameterDescriptor))
    }
    else if (classifier is ClassDescriptor && classifier.getClassObjectDescriptor() != null) {
        checkClassObjectVisibility(context)
        context.trace.record(EXPRESSION_TYPE, expression, classifier.getClassObjectType())
    }
}

private fun QualifierReceiver.resolveReferenceTarget(selector: DeclarationDescriptor?): DeclarationDescriptor {
    if (classifier is TypeParameterDescriptor) {
        return classifier
    }

    val containingDeclaration = when {
        selector is ConstructorDescriptor -> selector.getContainingDeclaration().getContainingDeclaration()
        else -> selector?.getContainingDeclaration()
    }

    if (packageView != null && (containingDeclaration is PackageFragmentDescriptorImpl || containingDeclaration is PackageViewDescriptor)
            && getFqName(packageView) == getFqName(containingDeclaration)) {
        return packageView
    }

    return descriptor
}

private fun QualifierReceiver.checkClassObjectVisibility(context: ExpressionTypingContext) {
    if (classifier !is ClassDescriptor) return

    val scopeContainer = context.scope.getContainingDeclaration()
    val classObject = classifier.getClassObjectDescriptor()
    assert(classObject != null) { "This check should be done only for classes with class objects: " + classifier }
    if (!Visibilities.isVisible(classObject!!, scopeContainer)) {
        context.trace.report(INVISIBLE_MEMBER.on(expression, classObject, classObject.getVisibility(), scopeContainer))
    }
}


