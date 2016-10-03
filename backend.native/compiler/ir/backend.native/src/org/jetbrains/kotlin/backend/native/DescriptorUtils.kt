package org.jetbrains.kotlin.backend.native

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces

/**
 * List of all implemented interfaces (including those which implemented by a super class)
 */
internal val ClassDescriptor.implementedInterfaces: List<ClassDescriptor>
    get() {
        val superClassImplementedInterfaces = this.getSuperClassNotAny()?.implementedInterfaces ?: emptyList()
        return (superClassImplementedInterfaces + this.getSuperInterfaces()).distinctBy { it.classId }
    }


/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
internal val FunctionDescriptor.implementation: FunctionDescriptor
    get() {
        if (this.kind.isReal) {
            return this
        } else {
            val overridden = OverridingUtil.getOverriddenDeclarations(this)
            val filtered = OverridingUtil.filterOutOverridden(overridden)
            return filtered.first { it.modality != Modality.ABSTRACT } as FunctionDescriptor
        }
    }