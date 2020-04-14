/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.descriptors.WrappedClassDescriptor

interface FirMetadataSource : MetadataSource {
    class Class(
        val klass: FirClass<*>, descriptor: WrappedClassDescriptor
    ) : MetadataSource.Class(descriptor), FirMetadataSource

    class Function(
        val function: FirFunction<*>, descriptor: FunctionDescriptor
    ) : MetadataSource.Function(descriptor), FirMetadataSource

    class Variable(
        val variable: FirVariable<*>,
        descriptor: PropertyDescriptor
    ) : MetadataSource.Property(descriptor), FirMetadataSource
}