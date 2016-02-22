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

package org.jetbrains.kotlin.resolve.scopes.receivers

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.FilteringScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.ScopeUtils
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

interface Qualifier: Receiver {

    val expression: KtExpression

    val referenceExpression: KtSimpleNameExpression

    val descriptor: DeclarationDescriptor

    val scope: MemberScope
}

abstract class QualifierReceiver(
        override val referenceExpression: KtSimpleNameExpression
) : Qualifier {

    override val expression: KtExpression
        get() = referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression

    abstract fun getNestedClassesAndPackageMembersScope(): MemberScope
}

class PackageQualifier(
        referenceExpression: KtSimpleNameExpression,
        val packageView: PackageViewDescriptor
) : QualifierReceiver(referenceExpression) {

    override val descriptor: DeclarationDescriptor
        get() = packageView

    override val scope: MemberScope get() = packageView.memberScope

    override fun getNestedClassesAndPackageMembersScope(): MemberScope = packageView.memberScope

    override fun toString() = "Package{$packageView}"
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

    override fun getNestedClassesAndPackageMembersScope(): MemberScope = MemberScope.Empty

    override val scope: MemberScope = MemberScope.Empty
}

class ClassQualifier(
        referenceExpression: KtSimpleNameExpression,
        override val classifier: ClassDescriptor,
        val classValueReceiver: ReceiverValue?
) : ClassifierQualifier(referenceExpression) {

    override val scope: MemberScope get() {
        if (classifier !is ClassDescriptor) {
            return MemberScope.Empty
        }

        val scopes = ArrayList<MemberScope>(3)

        val classObjectTypeScope = classifier.classValueType?.memberScope?.let {
            FilteringScope(it) { it !is ClassDescriptor }
        }
        scopes.addIfNotNull(classObjectTypeScope)

        scopes.add(classifier.staticScope)

        if (classifier.kind != ClassKind.ENUM_ENTRY) {
            scopes.add(classifier.unsubstitutedInnerClassesScope)
        }

        return ChainedMemberScope("Member scope for ${classifier.name} as class or object", scopes)
    }

    override fun getNestedClassesAndPackageMembersScope(): MemberScope {
        if (classifier !is ClassDescriptor) {
            return MemberScope.Empty
        }

        val scopes = ArrayList<MemberScope>(2)

        scopes.add(classifier.staticScope)

        if (classifier.kind != ClassKind.ENUM_ENTRY) {
            scopes.add(ScopeUtils.getStaticNestedClassesScope(classifier))
        }

        return ChainedMemberScope("Static scope for ${classifier.name} as class or object", scopes)
    }

    override fun toString() = "Class{$classifier}"
}

fun createClassifierQualifier(
        referenceExpression: KtSimpleNameExpression,
        classifier: ClassifierDescriptor,
        bindingContext: BindingContext
): ClassifierQualifier {
    val companionObjectReceiver = (classifier as? ClassDescriptor)?.classValueType?.let {
        ExpressionReceiver.create(referenceExpression, it, bindingContext)
    }
    return if (classifier is ClassDescriptor)
        ClassQualifier(referenceExpression, classifier, companionObjectReceiver)
    else
        ClassifierQualifierWithEmptyScope(referenceExpression, classifier)
}
