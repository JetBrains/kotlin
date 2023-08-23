/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.builtins.StandardNames.CONTINUATION_INTERFACE_FQ_NAME
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.isValueClass
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.sure
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun ModuleDescriptor.resolveClassByFqName(fqName: FqName, lookupLocation: LookupLocation): ClassDescriptor? {
    if (fqName.isRoot) return null

    (getPackage(fqName.parent())
            .memberScope.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassDescriptor)?.let { return it }

    return resolveClassByFqName(fqName.parent(), lookupLocation)
            ?.unsubstitutedInnerClassesScope
            ?.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassDescriptor
}

fun ModuleDescriptor.findContinuationClassDescriptorOrNull(lookupLocation: LookupLocation): ClassDescriptor? =
    resolveClassByFqName(CONTINUATION_INTERFACE_FQ_NAME, lookupLocation)

fun ModuleDescriptor.findContinuationClassDescriptor(lookupLocation: LookupLocation) =
    findContinuationClassDescriptorOrNull(lookupLocation).sure { "Continuation interface is not found" }

fun ModuleDescriptor.getContinuationOfTypeOrAny(kotlinType: KotlinType) =
    module.findContinuationClassDescriptorOrNull(
        NoLookupLocation.FROM_DESERIALIZATION
    )?.defaultType?.let {
        KotlinTypeFactory.simpleType(
            it,
            arguments = listOf(kotlinType.asTypeProjection())
        )
    } ?: module.builtIns.nullableAnyType

fun DeclarationDescriptor.isTopLevelInPackage(name: String, packageName: String): Boolean {
    if (name != this.name.asString()) return false

    val containingDeclaration = containingDeclaration as? PackageFragmentDescriptor ?: return false
    val packageFqName = containingDeclaration.fqName.asString()
    return packageName == packageFqName
}

fun DeclarationDescriptor.isTopLevelInPackage() = containingDeclaration is PackageFragmentDescriptor

fun DeclarationDescriptor.getTopLevelContainingClassifier(): ClassifierDescriptor? {
    val containingDeclaration = containingDeclaration

    if (containingDeclaration == null || this is PackageFragmentDescriptor) return null

    return if (!containingDeclaration.isTopLevelInPackage()) {
        containingDeclaration.getTopLevelContainingClassifier()
    } else if (containingDeclaration is ClassifierDescriptor) {
        containingDeclaration
    } else null
}

fun CallableDescriptor.isSupportedForCallableReference() = this is PropertyDescriptor || this is FunctionDescriptor

@OptIn(ExperimentalContracts::class)
fun DeclarationDescriptor.isSealed(): Boolean {
    contract {
        returns(true) implies (this@isSealed is ClassDescriptor)
    }
    return DescriptorUtils.isSealedClass(this)
}

fun DeclarationDescriptor.containingPackage(): FqName? {
    var container = containingDeclaration
    while (true) {
        if (container == null || container is PackageFragmentDescriptor) break
        container = container.containingDeclaration
    }
    require(container is PackageFragmentDescriptor?)
    return container?.fqName
}

object DeserializedDeclarationsFromSupertypeConflictDataKey : CallableDescriptor.UserDataKey<CallableMemberDescriptor>

fun FunctionDescriptor.isTypedEqualsInValueClass(): Boolean {
    val valueClassStarProjection =
        (containingDeclaration as? ClassDescriptor)?.takeIf { it.isValueClass() }?.defaultType?.replaceArgumentsWithStarProjections()
            ?: return false
    val returnType = returnType ?: return false
    return name == OperatorNameConventions.EQUALS
            && (returnType.isBoolean() || returnType.isNothing())
            && valueParameters.size == 1 && valueParameters[0].type.replaceArgumentsWithStarProjections() == valueClassStarProjection
            && contextReceiverParameters.isEmpty() && extensionReceiverParameter == null
}


fun FunctionDescriptor.overridesEqualsFromAny(): Boolean = name == OperatorNameConventions.EQUALS
        && valueParameters.size == 1 && valueParameters[0].type.isNullableAny()
        && contextReceiverParameters.isEmpty() && extensionReceiverParameter == null

tailrec fun DeclarationDescriptor.findPackage(): PackageFragmentDescriptor {
    return if (this is PackageFragmentDescriptor) this
    else this.containingDeclaration!!.findPackage()
}