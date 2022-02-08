/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

@DefaultImplementation(impl = AdditionalClassPartsProvider.Default::class)
interface AdditionalClassPartsProvider {
    fun generateAdditionalMethods(
        thisDescriptor: ClassDescriptor,
        result: MutableCollection<SimpleFunctionDescriptor>,
        name: Name,
        location: LookupLocation,
        fromSupertypes: Collection<SimpleFunctionDescriptor>
    )

    fun getAdditionalSupertypes(
        thisDescriptor: ClassDescriptor,
        existingSupertypes: List<KotlinType>
    ): List<KotlinType>

    object Default : AdditionalClassPartsProvider {
        override fun generateAdditionalMethods(
            thisDescriptor: ClassDescriptor,
            result: MutableCollection<SimpleFunctionDescriptor>,
            name: Name,
            location: LookupLocation,
            fromSupertypes: Collection<SimpleFunctionDescriptor>
        ) {}

        override fun getAdditionalSupertypes(
            thisDescriptor: ClassDescriptor,
            existingSupertypes: List<KotlinType>
        ): List<KotlinType> = emptyList()
    }
}
