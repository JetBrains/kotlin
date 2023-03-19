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

    context(LazyJavaResolverContext)
    fun getMethodNames(thisDescriptor: ClassDescriptor): List<Name>

    context(LazyJavaResolverContext)
    fun generateMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    )

    context(LazyJavaResolverContext)
    fun getStaticFunctionNames(thisDescriptor: ClassDescriptor): List<Name>

    context(LazyJavaResolverContext)
    fun generateStaticFunctions(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    )

    context(LazyJavaResolverContext)
    fun generateConstructors(thisDescriptor: ClassDescriptor, result: MutableList<ClassConstructorDescriptor>)

    context(LazyJavaResolverContext)
    fun getNestedClassNames(thisDescriptor: ClassDescriptor): List<Name>

    context(LazyJavaResolverContext)
    fun generateNestedClass(thisDescriptor: ClassDescriptor, name: Name, result: MutableList<ClassDescriptor>)

    context(LazyJavaResolverContext)
    fun modifyField(thisDescriptor: ClassDescriptor, propertyDescriptor: PropertyDescriptorImpl): PropertyDescriptorImpl
}

@Suppress("IncorrectFormatting") // KTIJ-22227
class CompositeSyntheticJavaPartsProvider(private val inner: List<SyntheticJavaPartsProvider>) : SyntheticJavaPartsProvider {
    context(LazyJavaResolverContext)
    override fun getMethodNames(thisDescriptor: ClassDescriptor): List<Name> {
        return inner.flatMap { it.getMethodNames(thisDescriptor) }
    }

    context(LazyJavaResolverContext)
    override fun generateMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        inner.forEach { it.generateMethods(thisDescriptor, name, result) }
    }

    context(LazyJavaResolverContext)
    override fun getStaticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        inner.flatMap { it.getStaticFunctionNames(thisDescriptor) }

    context(LazyJavaResolverContext)
    override fun generateStaticFunctions(thisDescriptor: ClassDescriptor, name: Name, result: MutableCollection<SimpleFunctionDescriptor>) {
        inner.forEach { it.generateStaticFunctions(thisDescriptor, name, result) }
    }

    context(LazyJavaResolverContext)
    override fun generateConstructors(thisDescriptor: ClassDescriptor, result: MutableList<ClassConstructorDescriptor>) {
        inner.forEach { it.generateConstructors(thisDescriptor, result) }
    }

    context(LazyJavaResolverContext)
    override fun getNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> {
        return inner.flatMap { it.getNestedClassNames(thisDescriptor) }
    }

    context(LazyJavaResolverContext)
    override fun generateNestedClass(thisDescriptor: ClassDescriptor, name: Name, result: MutableList<ClassDescriptor>) {
        inner.forEach { it.generateNestedClass(thisDescriptor, name, result) }
    }

    context(LazyJavaResolverContext)
    override fun modifyField(
        thisDescriptor: ClassDescriptor,
        propertyDescriptor: PropertyDescriptorImpl
    ): PropertyDescriptorImpl {
        return inner.fold(propertyDescriptor) { property, provider -> provider.modifyField(thisDescriptor, property) }
    }
}
