/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.name.Name

interface SyntheticJavaPartsProvider {

    companion object {
        val EMPTY = CompositeSyntheticJavaPartsProvider(emptyList())
    }

    fun getMethodNames(thisDescriptor: ClassDescriptor, c: LazyJavaResolverContext): List<Name>

    fun generateMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>,
        c: LazyJavaResolverContext,
    )

    fun getStaticFunctionNames(thisDescriptor: ClassDescriptor, c: LazyJavaResolverContext): List<Name>

    fun generateStaticFunctions(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>,
        c: LazyJavaResolverContext,
    )

    fun generateConstructors(thisDescriptor: ClassDescriptor, result: MutableList<ClassConstructorDescriptor>, c: LazyJavaResolverContext)

    fun getNestedClassNames(thisDescriptor: ClassDescriptor, c: LazyJavaResolverContext): List<Name>

    fun generateNestedClass(thisDescriptor: ClassDescriptor, name: Name, result: MutableList<ClassDescriptor>, c: LazyJavaResolverContext)

    fun modifyField(
        thisDescriptor: ClassDescriptor,
        propertyDescriptor: PropertyDescriptorImpl,
        c: LazyJavaResolverContext,
    ): PropertyDescriptorImpl
}

@Suppress("IncorrectFormatting") // KTIJ-22227
class CompositeSyntheticJavaPartsProvider(private val inner: List<SyntheticJavaPartsProvider>) : SyntheticJavaPartsProvider {
    override fun getMethodNames(thisDescriptor: ClassDescriptor, c: LazyJavaResolverContext): List<Name> {
        return inner.flatMap { it.getMethodNames(thisDescriptor, c) }
    }

    override fun generateMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>,
        c: LazyJavaResolverContext,
    ) {
        inner.forEach { it.generateMethods(thisDescriptor, name, result, c) }
    }

    override fun getStaticFunctionNames(thisDescriptor: ClassDescriptor, c: LazyJavaResolverContext): List<Name> =
        inner.flatMap { it.getStaticFunctionNames(thisDescriptor, c) }

    override fun generateStaticFunctions(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>,
        c: LazyJavaResolverContext,
    ) {
        inner.forEach { it.generateStaticFunctions(thisDescriptor, name, result, c) }
    }

    override fun generateConstructors(
        thisDescriptor: ClassDescriptor,
        result: MutableList<ClassConstructorDescriptor>,
        c: LazyJavaResolverContext,
    ) {
        inner.forEach { it.generateConstructors(thisDescriptor, result, c) }
    }

    override fun getNestedClassNames(thisDescriptor: ClassDescriptor, c: LazyJavaResolverContext): List<Name> {
        return inner.flatMap { it.getNestedClassNames(thisDescriptor, c) }
    }

    override fun generateNestedClass(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableList<ClassDescriptor>,
        c: LazyJavaResolverContext,
    ) {
        inner.forEach { it.generateNestedClass(thisDescriptor, name, result, c) }
    }

    override fun modifyField(
        thisDescriptor: ClassDescriptor,
        propertyDescriptor: PropertyDescriptorImpl,
        c: LazyJavaResolverContext,
    ): PropertyDescriptorImpl {
        return inner.fold(propertyDescriptor) { property, provider -> provider.modifyField(thisDescriptor, property, c) }
    }
}
