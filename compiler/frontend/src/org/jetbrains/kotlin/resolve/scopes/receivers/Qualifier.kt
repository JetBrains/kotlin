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
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.FilteringScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.ScopeUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

interface QualifierReceiver : Receiver {
    val staticScope: MemberScope

    val classValueReceiver: ReceiverValue?
}

abstract class Qualifier(val referenceExpression: KtSimpleNameExpression) : QualifierReceiver {
    val expression: KtExpression
        get() = referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression

    abstract val descriptor: DeclarationDescriptor

    abstract val scope: MemberScope
}

class PackageQualifier(
        referenceExpression: KtSimpleNameExpression,
        override val descriptor: PackageViewDescriptor
) : Qualifier(referenceExpression) {

    override val scope: MemberScope get() = descriptor.memberScope

    override val staticScope: MemberScope get() = descriptor.memberScope

    override val classValueReceiver: ReceiverValue? get() = null

    override fun toString() = "Package{$descriptor}"
}

class TypeParameterQualifier(
        referenceExpression: KtSimpleNameExpression,
        override val descriptor: TypeParameterDescriptor
) : Qualifier(referenceExpression) {
    override val scope: MemberScope get() = MemberScope.Empty
    override val staticScope: MemberScope get() = MemberScope.Empty
    override val classValueReceiver: ReceiverValue? get() = null
}

class ClassQualifier(
        referenceExpression: KtSimpleNameExpression,
        override val descriptor: ClassDescriptor
) : Qualifier(referenceExpression) {
    override val classValueReceiver: ClassValueReceiver? = descriptor.classValueType?.let {
        ClassValueReceiver(this, it)
    }

    override val scope: MemberScope get() {
        val scopes = ArrayList<MemberScope>(3)

        val classObjectTypeScope = descriptor.classValueType?.memberScope?.let {
            FilteringScope(it) { it !is ClassDescriptor }
        }
        scopes.addIfNotNull(classObjectTypeScope)

        scopes.add(descriptor.staticScope)

        if (descriptor.kind != ClassKind.ENUM_ENTRY) {
            scopes.add(descriptor.unsubstitutedInnerClassesScope)
        }

        return ChainedMemberScope("Member scope for ${descriptor.name} as class or object", scopes)
    }

    override val staticScope: MemberScope get() {
        val scopes = ArrayList<MemberScope>(2)

        scopes.add(descriptor.staticScope)

        if (descriptor.kind != ClassKind.ENUM_ENTRY) {
            scopes.add(ScopeUtils.getStaticNestedClassesScope(descriptor))
        }

        return ChainedMemberScope("Static scope for ${descriptor.name} as class or object", scopes)
    }

    override fun toString() = "Class{$descriptor}"
}

class ClassValueReceiver(val classQualifier: ClassQualifier, private val type: KotlinType) : ExpressionReceiver {
    override fun getType() = type

    override val expression: KtExpression
        get() = classQualifier.expression
}
