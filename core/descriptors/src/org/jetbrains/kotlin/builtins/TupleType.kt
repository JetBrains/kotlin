/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.*

object TupleType {
    @JvmStatic
    val classId = ClassId.fromString("kotlin/Tuple")

    @JvmStatic
    fun isTupleClass(descriptor: DeclarationDescriptor): Boolean {
        val container = descriptor.containingDeclaration
        return container is PackageFragmentDescriptor
                && container.fqName == KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
                && descriptor.name == classId.shortClassName
    }

    @JvmStatic
    fun isTupleType(type: KotlinType): Boolean {
        if (TypeUtils.noExpectedType(type)) return false

        val descriptor = type.constructor.declarationDescriptor ?: return false
        return isTupleClass(descriptor)
    }

    @JvmStatic
    fun getTupleClassDescriptor(moduleDescriptor: ModuleDescriptor): ClassDescriptor? {
        return moduleDescriptor.findClassAcrossModuleDependencies(classId)
    }

    @JvmStatic
    fun createType(
        moduleDescriptor: ModuleDescriptor,
        tupleTypeArgument: TypeProjection? = null
    ): KotlinType {
        val arguments = if (tupleTypeArgument == null) listOf(
            TypeProjectionImpl(
                Variance.INVARIANT,
                moduleDescriptor.builtIns.nullableAnyType
            )
        ) else listOf(tupleTypeArgument)
        return KotlinTypeFactory.simpleNotNullType(
            annotations = Annotations.EMPTY,
            arguments = arguments,
            descriptor = getTupleClassDescriptor(moduleDescriptor) ?: error { "Tuple class descriptor not found" }
        )
    }
}