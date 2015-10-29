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
import org.jetbrains.kotlin.diagnostics.Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND
import org.jetbrains.kotlin.diagnostics.Errors.NO_COMPANION_OBJECT
import org.jetbrains.kotlin.diagnostics.Errors.TYPE_PARAMETER_IS_NOT_AN_EXPRESSION
import org.jetbrains.kotlin.diagnostics.Errors.TYPE_PARAMETER_ON_LHS_OF_DOT
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.BindingContext.QUALIFIER
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName
import org.jetbrains.kotlin.resolve.bindingContextUtil.recordScope
import org.jetbrains.kotlin.resolve.descriptorUtil.classObjectType
import org.jetbrains.kotlin.resolve.descriptorUtil.hasClassObjectType
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.FilteringScope
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.resolve.scopes.utils.asKtScope
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*
import kotlin.properties.Delegates

public interface Qualifier: ReceiverValue {

    public val expression: KtExpression

    public val packageView: PackageViewDescriptor?

    public val classifier: ClassifierDescriptor?

    public val name: Name
        get() = classifier?.getName() ?: packageView!!.getName()

    // package, classifier or companion object descriptor
    public val resultingDescriptor: DeclarationDescriptor

    public val scope: KtScope
}

abstract class QualifierReceiver(
        val referenceExpression: KtSimpleNameExpression
) : Qualifier {

    override val expression: KtExpression = referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression

    val descriptor: DeclarationDescriptor
        get() = classifier ?: packageView ?: throw AssertionError("PackageView and classifier both are null")

    override var resultingDescriptor: DeclarationDescriptor by Delegates.notNull()

    fun getClassObjectReceiver(): ReceiverValue =
            (classifier as? ClassDescriptor)?.classObjectType?.let { ExpressionReceiver(referenceExpression, it) }
            ?: ReceiverValue.NO_RECEIVER

    abstract fun getNestedClassesAndPackageMembersScope(): KtScope

    override fun getType(): KotlinType = throw IllegalStateException("No type corresponds to QualifierReceiver '$this'")

    override fun exists() = true
}

class PackageQualifier(
        referenceExpression: KtSimpleNameExpression,
        override val packageView: PackageViewDescriptor
) : QualifierReceiver(referenceExpression) {

    override val classifier: ClassifierDescriptor? get() = null

    override val scope: KtScope get() = packageView.memberScope

    override fun getNestedClassesAndPackageMembersScope(): KtScope = packageView.memberScope

    override fun toString() = "Package{$packageView}"
}

class ClassifierQualifier(
        referenceExpression: KtSimpleNameExpression,
        override val classifier: ClassifierDescriptor
) : QualifierReceiver(referenceExpression) {

    override val packageView: PackageViewDescriptor? get() = null

    override val scope: KtScope get() {
        if (classifier !is ClassDescriptor) {
            return KtScope.Empty
        }

        val scopes = ArrayList<KtScope>(3)

        val classObjectTypeScope = classifier.classObjectType?.memberScope?.let {
            FilteringScope(it) { it !is ClassDescriptor }
        }
        scopes.addIfNotNull(classObjectTypeScope)

        scopes.add(classifier.staticScope)

        if (classifier.kind != ClassKind.ENUM_ENTRY) {
            scopes.add(classifier.unsubstitutedInnerClassesScope)
        }

        return ChainedScope(descriptor, "Member scope for $name as class or object", *scopes.toTypedArray())
    }

    override fun getNestedClassesAndPackageMembersScope(): KtScope {
        if (classifier !is ClassDescriptor) {
            return KtScope.Empty
        }

        val scopes = ArrayList<KtScope>(2)

        scopes.add(classifier.staticScope)

        if (classifier.kind != ClassKind.ENUM_ENTRY) {
            scopes.add(DescriptorUtils.getStaticNestedClassesScope(classifier))
        }

        return ChainedScope(descriptor, "Static scope for $name as class or object", *scopes.toTypedArray())
    }

    override fun toString() = "Classifier{$classifier}"
}


fun createQualifier(
        expression: KtSimpleNameExpression,
        receiver: ReceiverValue,
        context: ExpressionTypingContext
): QualifierReceiver? {
    val receiverScope = when {
        !receiver.exists() -> context.scope.asKtScope()
        receiver is QualifierReceiver -> receiver.scope
        else -> receiver.getType().getMemberScope()
    }

    val name = expression.getReferencedNameAsName()
    val packageViewDescriptor = receiverScope.getPackage(name)
    val classifierDescriptor = receiverScope.getClassifier(name)

    if (packageViewDescriptor == null && classifierDescriptor == null) return null

    context.trace.recordScope(context.scope, expression)

    val qualifier =
            if (receiver is PackageQualifier)
                if (packageViewDescriptor != null)
                    PackageQualifier(expression, packageViewDescriptor)
                else
                    ClassifierQualifier(expression, classifierDescriptor!!)
            else
                if (classifierDescriptor != null)
                    ClassifierQualifier(expression, classifierDescriptor)
                else
                    PackageQualifier(expression, packageViewDescriptor!!)

    context.trace.record(QUALIFIER, qualifier.expression, qualifier)
    return qualifier
}

fun QualifierReceiver.resolveAsStandaloneExpression(
        context: ExpressionTypingContext,
        symbolUsageValidator: SymbolUsageValidator
): KotlinType? {
    val classifier = this.classifier

    resolveAndRecordReferenceTarget(context, symbolUsageValidator, selector = null)
    if (classifier is TypeParameterDescriptor) {
        context.trace.report(TYPE_PARAMETER_IS_NOT_AN_EXPRESSION.on(referenceExpression, classifier))
    }
    else if (classifier is ClassDescriptor && !classifier.hasClassObjectType) {
        context.trace.report(NO_COMPANION_OBJECT.on(referenceExpression, classifier))
    }
    else if (packageView != null) {
        context.trace.report(EXPRESSION_EXPECTED_PACKAGE_FOUND.on(referenceExpression))
    }
    return null
}

fun QualifierReceiver.resolveAsReceiverInQualifiedExpression(
        context: ExpressionTypingContext,
        symbolUsageValidator: SymbolUsageValidator,
        selector: DeclarationDescriptor?
) {
    val classifier = this.classifier

    resolveAndRecordReferenceTarget(context, symbolUsageValidator, selector)
    if (classifier is TypeParameterDescriptor) {
        context.trace.report(TYPE_PARAMETER_ON_LHS_OF_DOT.on(referenceExpression, classifier))
    }
    else if (classifier is ClassDescriptor && classifier.hasClassObjectType) {
        context.trace.recordType(expression, classifier.classObjectType)
    }
}

private fun QualifierReceiver.resolveAndRecordReferenceTarget(
        context: ExpressionTypingContext,
        symbolUsageValidator: SymbolUsageValidator,
        selector: DeclarationDescriptor?
) {
    resultingDescriptor = resolveReferenceTarget(context, symbolUsageValidator, selector)
    context.trace.record(REFERENCE_TARGET, referenceExpression, resultingDescriptor)
}

private fun QualifierReceiver.resolveReferenceTarget(
        context: ExpressionTypingContext,
        symbolUsageValidator: SymbolUsageValidator,
        selector: DeclarationDescriptor?
): DeclarationDescriptor {
    val classifier = this.classifier
    val packageView = this.packageView

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

    val isCallableWithReceiver = selector is CallableDescriptor &&
                                 (selector.getDispatchReceiverParameter() != null || selector.getExtensionReceiverParameter() != null)

    val declarationDescriptor = descriptor
    if (declarationDescriptor is ClassifierDescriptor)
        symbolUsageValidator.validateTypeUsage(declarationDescriptor, context.trace, referenceExpression)

    if (isCallableWithReceiver && classifier is ClassDescriptor && classifier.hasClassObjectType) {
        val companionObjectDescriptor = classifier.getCompanionObjectDescriptor()
        if (companionObjectDescriptor != null) {
            context.trace.record(SHORT_REFERENCE_TO_COMPANION_OBJECT, referenceExpression, classifier)
            symbolUsageValidator.validateTypeUsage(companionObjectDescriptor, context.trace, referenceExpression)
            return companionObjectDescriptor
        }
    }

    return declarationDescriptor
}
