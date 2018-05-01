/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.sam

import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.resolve.calls.components.SamTypeTransformer
import org.jetbrains.kotlin.types.UnwrappedType

class JvmSamTypeTransformer(private val samResolver: SamConversionResolver) : SamTypeTransformer {

    override fun getFunctionTypeForPossibleSamType(possibleSamType: UnwrappedType): UnwrappedType? =
        SingleAbstractMethodUtils.getFunctionTypeForSamType(possibleSamType, samResolver)?.unwrap()

}