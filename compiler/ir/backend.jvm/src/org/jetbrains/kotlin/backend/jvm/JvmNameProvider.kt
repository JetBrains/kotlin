/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.codegen.getNameForDestructuredParameterOrNull
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.util.NameProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object JvmNameProvider : NameProvider {
    override fun nameForDeclaration(descriptor: DeclarationDescriptor): Name {
        if (descriptor is ValueParameterDescriptor)
            return nameForValueParameter(descriptor)
        return NameProvider.DEFAULT.nameForDeclaration(descriptor)
    }

    private fun nameForValueParameter(descriptor: ValueParameterDescriptor): Name {
        getNameForDestructuredParameterOrNull(descriptor)?.let { return Name.identifier(it) }
        if (DescriptorToSourceUtils.getSourceFromDescriptor(descriptor)?.safeAs<KtParameter>()?.isSingleUnderscore == true) {
            return Name.identifier("\$noName_${descriptor.index}")
        }
        return descriptor.name
    }
}
