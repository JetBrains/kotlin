/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.AdditionalClassPartsProvider
import org.jetbrains.kotlin.resolve.FunctionsFromAny
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.resolve.jvm.annotations.isJvmRecord
import org.jetbrains.kotlin.types.KotlinType

class JvmAdditionalClassPartsProvider : AdditionalClassPartsProvider {
    override fun generateAdditionalMethods(
        thisDescriptor: ClassDescriptor,
        result: MutableCollection<SimpleFunctionDescriptor>,
        name: Name,
        location: LookupLocation,
        fromSupertypes: Collection<SimpleFunctionDescriptor>
    ) {
        if (thisDescriptor.isJvmRecord()) {
            FunctionsFromAny.addFunctionFromAnyIfNeeded(thisDescriptor, result, name, fromSupertypes)
        }
    }

    override fun getAdditionalSupertypes(
        thisDescriptor: ClassDescriptor,
        existingSupertypes: List<KotlinType>
    ): List<KotlinType> {
        if (thisDescriptor.isJvmRecord() && existingSupertypes.none(::isJavaLangRecordType)) {
            thisDescriptor.module.resolveTopLevelClass(JAVA_LANG_RECORD_FQ_NAME, NoLookupLocation.FOR_DEFAULT_IMPORTS)?.defaultType?.let {
                return listOf(it)
            }
        }

        return emptyList()
    }
}

private fun isJavaLangRecordType(it: KotlinType) =
    KotlinBuiltIns.isConstructedFromGivenClass(it, JAVA_LANG_RECORD_FQ_NAME)
