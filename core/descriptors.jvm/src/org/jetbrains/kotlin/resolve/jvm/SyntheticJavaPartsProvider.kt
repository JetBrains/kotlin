/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name

interface SyntheticJavaPartsProvider {

    companion object {
        val EMPTY = CompositeSyntheticJavaPartsProvider(emptyList())
    }

    fun getMethodNames(thisDescriptor: ClassDescriptor): List<Name>

    fun generateMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    )

    fun getStaticFunctionNames(thisDescriptor: ClassDescriptor): List<Name>

    fun generateStaticFunctions(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    )

    fun generateConstructors(thisDescriptor: ClassDescriptor, result: MutableList<ClassConstructorDescriptor>)

}

class CompositeSyntheticJavaPartsProvider(private val inner: List<SyntheticJavaPartsProvider>) : SyntheticJavaPartsProvider {
    override fun getMethodNames(thisDescriptor: ClassDescriptor): List<Name> =
        inner.flatMap { it.getMethodNames(thisDescriptor) }

    override fun generateMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) {
        inner.forEach { it.generateMethods(thisDescriptor, name, result) }
    }

    override fun getStaticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        inner.flatMap { it.getStaticFunctionNames(thisDescriptor) }

    override fun generateStaticFunctions(thisDescriptor: ClassDescriptor, name: Name, result: MutableCollection<SimpleFunctionDescriptor>) {
        inner.forEach { it.generateStaticFunctions(thisDescriptor, name, result) }
    }

    override fun generateConstructors(thisDescriptor: ClassDescriptor, result: MutableList<ClassConstructorDescriptor>) {
        inner.forEach { it.generateConstructors(thisDescriptor, result) }
    }
}
