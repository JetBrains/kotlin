package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

fun ClassDescriptor?.getter2Descriptor(methodName: Name) = this?.let {
    this.unsubstitutedMemberScope.getContributedDescriptors{true}
            .firstOrNull {
                it.name == methodName
            } ?.let {
        return@let (it as? PropertyDescriptor)?.getter
    }
}

fun ClassDescriptor?.signature2Descriptor(methodName: Name, signature:Array<KotlinType> = emptyArray()) = this?.let {
    this
            .unsubstitutedMemberScope
            .getContributedFunctions(methodName, NoLookupLocation.FROM_BACKEND)
            .firstOrNull {
                return@firstOrNull it.valueParameters.size == signature.size
                        && (signature.isEmpty() || it.valueParameters.any {
                    p -> val index = it.valueParameters.indexOf(p)
                    return@any p.type == signature[index]
        })
    }
}