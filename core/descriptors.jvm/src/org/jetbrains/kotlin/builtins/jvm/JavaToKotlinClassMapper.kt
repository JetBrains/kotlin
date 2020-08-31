/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.jvm

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMapper
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

object JavaToKotlinClassMapper : PlatformToKotlinClassMapper {
    override fun mapPlatformClass(classDescriptor: ClassDescriptor): Collection<ClassDescriptor> {
        val className = DescriptorUtils.getFqName(classDescriptor)
        return if (className.isSafe)
            mapPlatformClass(className.toSafe(), classDescriptor.builtIns)
        else
            emptySet()
    }

    fun mapPlatformClass(fqName: FqName, builtIns: KotlinBuiltIns): Collection<ClassDescriptor> {
        val kotlinAnalog = mapJavaToKotlin(fqName, builtIns) ?: return emptySet()

        val kotlinMutableAnalogFqName = JavaToKotlinClassMap.readOnlyToMutable(kotlinAnalog.fqNameUnsafe) ?: return setOf(kotlinAnalog)

        return listOf(kotlinAnalog, builtIns.getBuiltInClassByFqName(kotlinMutableAnalogFqName))
    }

    fun mapJavaToKotlin(fqName: FqName, builtIns: KotlinBuiltIns, functionTypeArity: Int? = null): ClassDescriptor? {
        val kotlinClassId =
            if (functionTypeArity != null && fqName == JavaToKotlinClassMap.FUNCTION_N_FQ_NAME) StandardNames.getFunctionClassId(functionTypeArity)
            else JavaToKotlinClassMap.mapJavaToKotlin(fqName)
        return if (kotlinClassId != null) builtIns.getBuiltInClassByFqName(kotlinClassId.asSingleFqName()) else null
    }

    fun isMutable(mutable: ClassDescriptor): Boolean = JavaToKotlinClassMap.isMutable(DescriptorUtils.getFqName(mutable))

    fun isMutable(type: KotlinType): Boolean {
        val classDescriptor = TypeUtils.getClassDescriptor(type)
        return classDescriptor != null && isMutable(classDescriptor)
    }

    fun isReadOnly(readOnly: ClassDescriptor): Boolean = JavaToKotlinClassMap.isReadOnly(DescriptorUtils.getFqName(readOnly))

    fun isReadOnly(type: KotlinType): Boolean {
        val classDescriptor = TypeUtils.getClassDescriptor(type)
        return classDescriptor != null && isReadOnly(classDescriptor)
    }

    fun convertMutableToReadOnly(mutable: ClassDescriptor): ClassDescriptor {
        return convertToOppositeMutability(mutable,"mutable") {
            JavaToKotlinClassMap.mutableToReadOnly(it)
        }
    }

    fun convertReadOnlyToMutable(readOnly: ClassDescriptor): ClassDescriptor {
        return convertToOppositeMutability(readOnly, "read-only") {
            JavaToKotlinClassMap.readOnlyToMutable(it)
        }
    }

    private inline fun convertToOppositeMutability(
        descriptor: ClassDescriptor,
        mutabilityKindName: String,
        oppositeNameExtractor: (FqNameUnsafe?) -> FqName?
    ): ClassDescriptor {
        val oppositeClassFqName = oppositeNameExtractor(DescriptorUtils.getFqName(descriptor))
            ?: throw IllegalArgumentException("Given class $descriptor is not a $mutabilityKindName collection")
        return descriptor.builtIns.getBuiltInClassByFqName(oppositeClassFqName)
    }
}
