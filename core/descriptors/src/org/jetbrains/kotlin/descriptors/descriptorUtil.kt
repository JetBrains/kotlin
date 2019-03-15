/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.sure

fun ModuleDescriptor.resolveClassByFqName(fqName: FqName, lookupLocation: LookupLocation): ClassDescriptor? {
    if (fqName.isRoot) return null

    (getPackage(fqName.parent())
            .memberScope.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassDescriptor)?.let { return it }

    return resolveClassByFqName(fqName.parent(), lookupLocation)
            ?.unsubstitutedInnerClassesScope
            ?.getContributedClassifier(fqName.shortName(), lookupLocation) as? ClassDescriptor
}

fun ModuleDescriptor.findContinuationClassDescriptorOrNull(lookupLocation: LookupLocation, releaseCoroutines: Boolean) =
    if (releaseCoroutines)
        resolveClassByFqName(DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME_RELEASE, lookupLocation)
    else
        resolveClassByFqName(DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME_EXPERIMENTAL, lookupLocation)

fun ModuleDescriptor.findContinuationClassDescriptor(lookupLocation: LookupLocation, releaseCoroutines: Boolean) =
    findContinuationClassDescriptorOrNull(lookupLocation, releaseCoroutines).sure { "Continuation interface is not found" }

fun ModuleDescriptor.getContinuationOfTypeOrAny(kotlinType: KotlinType, isReleaseCoroutines: Boolean) =
    module.findContinuationClassDescriptorOrNull(
        NoLookupLocation.FROM_DESERIALIZATION,
        isReleaseCoroutines
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