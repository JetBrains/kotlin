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

package org.jetbrains.kotlin.types.expressions.unqualifiedSuper

import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetSuperExpression
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import java.util.*

public fun resolveUnqualifiedSuperFromExpressionContext(superExpression: JetSuperExpression, supertypes: Collection<JetType>): Collection<JetType> {
    val parentElement = superExpression.getParent()

    if (parentElement is JetDotQualifiedExpression) {
        val selectorExpression = parentElement.getSelectorExpression()
        when (selectorExpression) {
            is JetCallExpression -> {
                // super.foo(...): foo can be a function or a property of a callable type
                val calleeExpression = selectorExpression.getCalleeExpression()
                if (calleeExpression is JetSimpleNameExpression) {
                    return resolveSupertypesByCalleeName(supertypes, calleeExpression.getReferencedNameAsName())
                }
            }
            is JetSimpleNameExpression -> {
                // super.x: x can be a property only
                return resolveSupertypesByPropertyName(supertypes, selectorExpression.getReferencedNameAsName())
            }
        }
    }

    return emptyList()
}

private fun resolveSupertypesByCalleeName(supertypes: Collection<JetType>, calleeName: Name): Collection<JetType> =
        resolveSupertypesByMembers(supertypes) { getFunctionMembers(it, calleeName) + getPropertyMembers(it, calleeName) }

private fun resolveSupertypesByPropertyName(supertypes: Collection<JetType>, propertyName: Name): Collection<JetType> =
        resolveSupertypesByMembers(supertypes) { getPropertyMembers(it, propertyName) }

private inline fun resolveSupertypesByMembers(
        supertypes: Collection<JetType>,
        getMembers: (JetType) -> Collection<MemberDescriptor>
): Collection<JetType> {
    val withConcreteMembers = SmartList<JetType>()
    val withAnyMembers = SmartList<JetType>()

    for (supertype in supertypes) {
        val members = getMembers(supertype)
        if (members.isNotEmpty()) {
            withAnyMembers.add(supertype)
            if (members any ::isConcreteMember) {
                withConcreteMembers.add(supertype)
            }
        }
    }

    return if (withConcreteMembers.isNotEmpty()) withConcreteMembers else withAnyMembers
}

private fun getFunctionMembers(type: JetType, name: Name): Collection<MemberDescriptor> =
        type.getMemberScope().getFunctions(name)

private fun getPropertyMembers(type: JetType, name: Name): Collection<MemberDescriptor> =
        type.getMemberScope().getProperties(name).filterIsInstanceTo(SmartList<MemberDescriptor>())

private fun isConcreteMember(memberDescriptor: MemberDescriptor): Boolean =
        memberDescriptor.getModality() != Modality.ABSTRACT
