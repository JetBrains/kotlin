/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.serialization

import org.jetbrains.kotlin.codegen.state.KotlinTypeMapperBase
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.DescriptorAwareStringTable

class JvmCodegenStringTable @JvmOverloads constructor(
    private val typeMapper: KotlinTypeMapperBase,
    nameResolver: JvmNameResolver? = null
) : JvmStringTable(nameResolver), DescriptorAwareStringTable {
    override fun getLocalClassIdReplacement(descriptor: ClassifierDescriptorWithTypeParameters): ClassId =
        when (val container = descriptor.containingDeclaration) {
            is ClassifierDescriptorWithTypeParameters -> getLocalClassIdReplacement(container).createNestedClassId(descriptor.name)
            is PackageFragmentDescriptor -> {
                throw IllegalStateException("getLocalClassIdReplacement should only be called for local classes: $descriptor")
            }
            else -> {
                val fqName = FqName(typeMapper.mapClass(descriptor).internalName.replace('/', '.'))
                ClassId(fqName.parent(), FqName.topLevel(fqName.shortName()), isLocal = true)
            }
        }

    override val isLocalClassIdReplacementKeptGeneric: Boolean
        get() = true
}
