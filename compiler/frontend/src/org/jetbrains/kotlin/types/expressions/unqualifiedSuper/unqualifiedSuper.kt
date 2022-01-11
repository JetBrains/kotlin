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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.isAny


fun resolveUnqualifiedSuperFromExpressionContext(
    superExpression: KtSuperExpression,
    supertypes: Collection<KotlinType>,
    anyType: KotlinType
): Pair<Collection<KotlinType>, Boolean> {
    val parentElement = superExpression.parent

    if (parentElement is KtDotQualifiedExpression) {
        when (val selectorExpression = parentElement.selectorExpression) {
            is KtCallExpression -> {
                // super.foo(...): foo can be a function or a property of a callable type
                val calleeExpression = selectorExpression.calleeExpression
                if (calleeExpression is KtSimpleNameExpression) {
                    val calleeName = calleeExpression.getReferencedNameAsName()
                    return if (isCallingMethodOfAny(selectorExpression, calleeName)) {
                        resolveSupertypesForMethodOfAny(supertypes, calleeName, anyType)
                    } else {
                        resolveSupertypesByCalleeName(supertypes, calleeName) to false
                    }
                }
            }
            is KtSimpleNameExpression -> {
                // super.x: x can be a property only
                // NB there are no properties in kotlin.Any
                return resolveSupertypesByPropertyName(supertypes, selectorExpression.getReferencedNameAsName()) to false
            }
        }
    }

    return emptyList<KotlinType>() to false
}

private val ARITY_OF_METHODS_OF_ANY = hashMapOf("hashCode" to 0, "equals" to 1, "toString" to 0)

private fun isCallingMethodOfAny(callExpression: KtCallExpression, calleeName: Name): Boolean =
    ARITY_OF_METHODS_OF_ANY.getOrElse(calleeName.asString()) { -1 } == callExpression.valueArguments.size

fun isPossiblyAmbiguousUnqualifiedSuper(superExpression: KtSuperExpression, supertypes: Collection<KotlinType>): Boolean =
    supertypes.size > 1 ||
            (supertypes.size == 1 && supertypes.single().isInterface() && isCallingMethodOfAnyWithSuper(superExpression))

private fun isCallingMethodOfAnyWithSuper(superExpression: KtSuperExpression): Boolean {
    val parent = superExpression.parent
    if (parent is KtDotQualifiedExpression) {
        val selectorExpression = parent.selectorExpression
        if (selectorExpression is KtCallExpression) {
            val calleeExpression = selectorExpression.calleeExpression
            if (calleeExpression is KtSimpleNameExpression) {
                val calleeName = calleeExpression.getReferencedNameAsName()
                return isCallingMethodOfAny(selectorExpression, calleeName)
            }
        }
    }

    return false
}

private val LOOKUP_LOCATION = NoLookupLocation.WHEN_GET_SUPER_MEMBERS

private fun KotlinType.isInterface(): Boolean =
    TypeUtils.getClassDescriptor(this)?.kind == ClassKind.INTERFACE

private fun resolveSupertypesForMethodOfAny(
    supertypes: Collection<KotlinType>,
    calleeName: Name,
    anyType: KotlinType
): Pair<Collection<KotlinType>, Boolean> {
    val (typesWithConcreteOverride, isEqualsMigration) = resolveSupertypesByMembers(supertypes, allowNonConcreteInterfaceMembers = false) {
        getFunctionMembers(it, calleeName)
    }
    return typesWithConcreteOverride.ifEmpty { listOf(anyType) } to isEqualsMigration
}

private fun resolveSupertypesByCalleeName(supertypes: Collection<KotlinType>, calleeName: Name): Collection<KotlinType> =
    resolveSupertypesByMembers(supertypes, allowNonConcreteInterfaceMembers = true) {
        getFunctionMembers(it, calleeName) +
                getPropertyMembers(it, calleeName)
    }.first

private fun resolveSupertypesByPropertyName(supertypes: Collection<KotlinType>, propertyName: Name): Collection<KotlinType> =
    resolveSupertypesByMembers(supertypes, allowNonConcreteInterfaceMembers = true) {
        getPropertyMembers(it, propertyName)
    }.first

private inline fun resolveSupertypesByMembers(
    supertypes: Collection<KotlinType>,
    allowNonConcreteInterfaceMembers: Boolean,
    getMembers: (KotlinType) -> Collection<CallableMemberDescriptor>
): Pair<Collection<KotlinType>, Boolean> {
    val typesWithConcreteMembers = SmartList<KotlinType>()
    val typesWithNonConcreteMembers = SmartList<KotlinType>()

    for (supertype in supertypes) {
        val members = getMembers(supertype)
        if (members.isNotEmpty()) {
            if (members.any { isConcreteMember(supertype, it) })
                typesWithConcreteMembers.add(supertype)
            else if (members.any { it.dispatchReceiverParameter?.type?.isAny() == false })
                typesWithNonConcreteMembers.add(supertype)
        }
    }

    typesWithConcreteMembers.removeAll { typeWithConcreteMember ->
        typesWithNonConcreteMembers.any { typeWithNonConcreteMember ->
            KotlinTypeChecker.DEFAULT.isSubtypeOf(typeWithNonConcreteMember, typeWithConcreteMember)
        }
    }

    return when {
        typesWithConcreteMembers.isNotEmpty() ->
            typesWithConcreteMembers to false
        allowNonConcreteInterfaceMembers ->
            typesWithNonConcreteMembers to false
        else ->
            typesWithNonConcreteMembers.filter {
                TypeUtils.getClassDescriptor(it)?.kind == ClassKind.CLASS
            } to true
    }
}

private fun getFunctionMembers(type: KotlinType, name: Name): Collection<CallableMemberDescriptor> =
    type.memberScope.getContributedFunctions(name, LOOKUP_LOCATION)

private fun getPropertyMembers(type: KotlinType, name: Name): Collection<CallableMemberDescriptor> =
    type.memberScope.getContributedVariables(name, LOOKUP_LOCATION).filterIsInstanceTo(SmartList())

private fun isConcreteMember(supertype: KotlinType, memberDescriptor: CallableMemberDescriptor): Boolean {
    // "Concrete member" is a function or a property that is not abstract,
    // and is not an implicit fake override for a method of Any on an interface.

    if (memberDescriptor.modality == Modality.ABSTRACT)
        return false

    val classDescriptorForSupertype = TypeUtils.getClassDescriptor(supertype)
    val memberKind = memberDescriptor.kind
    if (classDescriptorForSupertype?.kind == ClassKind.INTERFACE && memberKind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        // We have a fake override on interface. It should have a dispatch receiver, which should not be Any.
        val dispatchReceiverType = memberDescriptor.dispatchReceiverParameter?.type ?: return false
        val dispatchReceiverClass = TypeUtils.getClassDescriptor(dispatchReceiverType) ?: return false
        return !KotlinBuiltIns.isAny(dispatchReceiverClass)
    }

    return true
}
