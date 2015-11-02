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
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName
import org.jetbrains.kotlin.resolve.bindingContextUtil.recordScope
import org.jetbrains.kotlin.resolve.descriptorUtil.companionObjectType
import org.jetbrains.kotlin.resolve.descriptorUtil.hasCompanionObject
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.FilteringScope
import org.jetbrains.kotlin.resolve.scopes.JetScopeUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findPackage
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*
import kotlin.properties.Delegates

public interface Qualifier: Receiver {

    public val expression: KtExpression

    public val name: Name
        get() = descriptor.name

    public val descriptor: DeclarationDescriptor

    // package, classifier or companion object descriptor
    public val resultingDescriptor: DeclarationDescriptor

    public val scope: MemberScope
}

abstract class QualifierReceiver(
        val referenceExpression: KtSimpleNameExpression
) : Qualifier {

    override val expression: KtExpression
        get() = referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression

    override var resultingDescriptor: DeclarationDescriptor by Delegates.notNull()

    abstract fun getNestedClassesAndPackageMembersScope(): MemberScope

    override fun exists() = true
}

class PackageQualifier(
        referenceExpression: KtSimpleNameExpression,
        public val packageView: PackageViewDescriptor
) : QualifierReceiver(referenceExpression) {

    override val descriptor: DeclarationDescriptor
        get() = packageView

    override val scope: MemberScope get() = packageView.memberScope

    override fun getNestedClassesAndPackageMembersScope(): MemberScope = packageView.memberScope

    override fun toString() = "Package{$packageView}"

    override val companionObjectReceiver: ReceiverValue?
        get() = null
}

abstract class ClassifierQualifier(referenceExpression: KtSimpleNameExpression) : QualifierReceiver(referenceExpression) {
    abstract val classifier: ClassifierDescriptor

    override val descriptor: DeclarationDescriptor
        get() = classifier
}

class ClassifierQualifierWithEmptyScope(
        referenceExpression: KtSimpleNameExpression,
        override val classifier: ClassifierDescriptor
) : ClassifierQualifier(referenceExpression) {

    override fun getNestedClassesAndPackageMembersScope(): MemberScope = scope

    override val scope: MemberScope = MemberScope.empty(classifier)
}

class ClassQualifier(
        referenceExpression: KtSimpleNameExpression,
        override val classifier: ClassDescriptor,
        val companionObjectReceiver: ReceiverValue?
) : ClassifierQualifier(referenceExpression) {

    override val scope: MemberScope get() {
        if (classifier !is ClassDescriptor) {
            return MemberScope.Empty
        }

        val scopes = ArrayList<MemberScope>(3)

        val classObjectTypeScope = classifier.companionObjectType?.memberScope?.let {
            FilteringScope(it) { it !is ClassDescriptor }
        }
        scopes.addIfNotNull(classObjectTypeScope)

        scopes.add(classifier.staticScope)

        if (classifier.kind != ClassKind.ENUM_ENTRY) {
            scopes.add(classifier.unsubstitutedInnerClassesScope)
        }

        return ChainedScope("Member scope for $name as class or object", *scopes.toTypedArray())
    }

    override fun getNestedClassesAndPackageMembersScope(): MemberScope {
        if (classifier !is ClassDescriptor) {
            return MemberScope.Empty
        }

        val scopes = ArrayList<MemberScope>(2)

        scopes.add(classifier.staticScope)

        if (classifier.kind != ClassKind.ENUM_ENTRY) {
            scopes.add(JetScopeUtils.getStaticNestedClassesScope(classifier))
        }

        return ChainedScope("Static scope for $name as class or object", *scopes.toTypedArray())
    }

    fun getClassObjectReceiver(): ReceiverValue =
            classifier.classObjectType?.let { ExpressionReceiver(referenceExpression, it) } ?: ReceiverValue.NO_RECEIVER

    override fun toString() = "Class{$classifier}"
}

fun createQualifier(
        expression: KtSimpleNameExpression,
        receiver: Receiver,
        context: ExpressionTypingContext
): QualifierReceiver? {
    val receiverScope = when {
        !receiver.exists() -> context.scope
        receiver is QualifierReceiver -> receiver.scope.memberScopeAsImportingScope()
        receiver is ReceiverValue -> receiver.type.memberScope.memberScopeAsImportingScope()
        else -> throw IllegalArgumentException("Unexpected receiver kind: $receiver")
    }

    val name = expression.getReferencedNameAsName()
    val packageViewDescriptor = receiverScope.findPackage(name)
    val classifierDescriptor = receiverScope.findClassifier(name, KotlinLookupLocation(expression))

    if (packageViewDescriptor == null && classifierDescriptor == null) return null

    context.trace.recordScope(context.scope, expression)

    val qualifier =
            if (receiver is PackageQualifier) {
                if (packageViewDescriptor != null)
                    PackageQualifier(expression, packageViewDescriptor)
                else
                    createClassifierQualifier(expression, classifierDescriptor!!, context.trace.bindingContext)
            else
                if (classifierDescriptor != null)
                    createClassifierQualifier(expression, classifierDescriptor, context.trace.bindingContext)
                else
                    PackageQualifier(expression, packageViewDescriptor!!)
            }

    context.trace.record(QUALIFIER, qualifier.expression, qualifier)
    return qualifier
}

fun createClassifierQualifier(
        referenceExpression: KtSimpleNameExpression,
        classifier: ClassifierDescriptor,
        bindingContext: BindingContext
): ClassifierQualifier {
    val companionObjectReceiver = (classifier as? ClassDescriptor)?.companionObjectType?.let {
        ExpressionReceiver.create(referenceExpression, it, bindingContext)
    }
    return ClassQualifier(referenceExpression, classifier, companionObjectReceiver)
}