/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.Name

interface SyntheticJavaPartsProvider {

    companion object {
        val EMPTY = CompositeSyntheticJavaPartsProvider(emptyList())
    }

    fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name>

    fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    )

}

class CompositeSyntheticJavaPartsProvider(private val inner: List<SyntheticJavaPartsProvider>) : SyntheticJavaPartsProvider {
    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> =
        inner.flatMap { it.getSyntheticFunctionNames(thisDescriptor) }

    override fun generateSyntheticMethods(
        thisDescriptor: ClassDescriptor,
        name: Name,
        result: MutableCollection<SimpleFunctionDescriptor>
    ) = inner.forEach { it.generateSyntheticMethods(thisDescriptor, name, result) }
}
