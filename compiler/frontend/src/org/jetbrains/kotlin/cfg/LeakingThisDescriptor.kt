/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cfg

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject

interface LeakingThisDescriptor {
    val classOrObject: KtClassOrObject

    data class PropertyIsNull(val property: PropertyDescriptor, override val classOrObject: KtClassOrObject) : LeakingThisDescriptor

    data class NonFinalClass(val klass: ClassDescriptor, override val classOrObject: KtClassOrObject) : LeakingThisDescriptor

    data class NonFinalProperty(val property: PropertyDescriptor, override val classOrObject: KtClassOrObject) : LeakingThisDescriptor

    data class NonFinalFunction(val function: FunctionDescriptor, override val classOrObject: KtClassOrObject) : LeakingThisDescriptor
}
