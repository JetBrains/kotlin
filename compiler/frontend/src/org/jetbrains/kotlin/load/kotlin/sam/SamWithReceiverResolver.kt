/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin.sam

import org.jetbrains.kotlin.descriptors.FunctionDescriptor

interface SamWithReceiverResolver {
    fun shouldConvertFirstSamParameterToReceiver(function: FunctionDescriptor): Boolean
}