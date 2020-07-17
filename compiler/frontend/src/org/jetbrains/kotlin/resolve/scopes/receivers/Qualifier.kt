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
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

interface Qualifier : QualifierReceiver {
    val referenceExpression: KtSimpleNameExpression
}

val Qualifier.expression: KtExpression
    get() = referenceExpression.getTopmostParentQualifiedExpressionForSelector() ?: referenceExpression

class PackageQualifier(
    override val referenceExpression: KtSimpleNameExpression,
    override val descriptor: PackageViewDescriptor
) : Qualifier {
    override val classValueReceiver: ReceiverValue? get() = null
    override val staticScope: MemberScope get() = descriptor.memberScope

    override fun toString() = "Package{$descriptor}"
}

class TypeParameterQualifier(
    override val referenceExpression: KtSimpleNameExpression,
    override val descriptor: TypeParameterDescriptor
) : Qualifier {
    override val classValueReceiver: ReceiverValue? get() = null
    override val staticScope: MemberScope get() = MemberScope.Empty

    override fun toString() = "TypeParameter{$descriptor}"
}

interface ClassifierQualifier : Qualifier {
    override val descriptor: ClassifierDescriptorWithTypeParameters
}

class ClassQualifier(
    override val referenceExpression: KtSimpleNameExpression,
    override val descriptor: ClassDescriptor
) : ClassifierQualifier {
    override val classValueReceiver: ClassValueReceiver? = descriptor.classValueType?.let {
        ClassValueReceiver(this, it)
    }

    override val staticScope: MemberScope
        get() =
            if (descriptor.kind == ClassKind.ENUM_ENTRY) descriptor.staticScope
            else ChainedMemberScope.create(
                "Static scope for ${descriptor.name} as class or object",
                descriptor.staticScope,
                descriptor.unsubstitutedInnerClassesScope
            )

    override fun toString() = "Class{$descriptor}"
}

class TypeAliasQualifier(
    override val referenceExpression: KtSimpleNameExpression,
    override val descriptor: TypeAliasDescriptor,
    val classDescriptor: ClassDescriptor
) : ClassifierQualifier {
    override val classValueReceiver: ClassValueReceiver?
        get() = classDescriptor.classValueType?.let {
            ClassValueReceiver(this, it)
        }

    override val staticScope: MemberScope
        get() = when {
            DescriptorUtils.isEnumClass(classDescriptor) ->
                ChainedMemberScope.create(
                    "Static scope for typealias ${descriptor.name}",
                    classDescriptor.staticScope,
                    EnumEntriesScope()
                )
            else ->
                classDescriptor.staticScope
        }

    /**
     * We cannot use [org.jetbrains.kotlin.descriptors.ClassDescriptor.getUnsubstitutedMemberScope] directly,
     * because we do not allow complete resolve through type aliases yet (see KT-15298).
     *
     * However, we want to allow to resolve and autocomplete enum constants even through type aliases;
     * that's why we use [org.jetbrains.kotlin.descriptors.ClassDescriptor.getUnsubstitutedMemberScope],
     * but filter only enum entries.
     */
    private inner class EnumEntriesScope : MemberScopeImpl() {
        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> =
            classDescriptor.unsubstitutedInnerClassesScope
                .getContributedDescriptors(kindFilter, nameFilter)
                .filter { DescriptorUtils.isEnumEntry(it) }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
            classDescriptor.unsubstitutedInnerClassesScope
                .getContributedClassifier(name, location)
                ?.takeIf { DescriptorUtils.isEnumEntry(it) }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()
            p.println("descriptor = ", descriptor)
            p.popIndent()
            p.println("}")
        }
    }
}

class ClassValueReceiver @JvmOverloads constructor(
    val classQualifier: ClassifierQualifier,
    private val type: KotlinType,
    original: ClassValueReceiver? = null
) : ExpressionReceiver {
    private val original = original ?: this

    override fun getType() = type

    override val expression: KtExpression
        get() = classQualifier.expression

    override fun replaceType(newType: KotlinType) = ClassValueReceiver(classQualifier, newType, original)

    override fun getOriginal() = original
}
