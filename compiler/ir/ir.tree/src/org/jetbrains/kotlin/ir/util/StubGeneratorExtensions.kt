/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

open class StubGeneratorExtensions {
    open fun computeExternalDeclarationOrigin(descriptor: DeclarationDescriptor): IrDeclarationOrigin? = null

    open fun generateFacadeClass(source: DeserializedContainerSource): IrClass? = null

    open fun isPropertyWithPlatformField(descriptor: PropertyDescriptor): Boolean = false

    companion object {
        @JvmField
        val EMPTY = StubGeneratorExtensions()
    }
}
