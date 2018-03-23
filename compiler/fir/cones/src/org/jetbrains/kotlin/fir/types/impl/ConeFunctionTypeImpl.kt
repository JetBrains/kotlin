/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.fir.types.ConeFunctionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType

class ConeFunctionTypeImpl(
    override val receiverType: ConeKotlinType?,
    override val parameterTypes: List<ConeKotlinType>,
    override val returnType: ConeKotlinType
) : ConeFunctionType()