/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirBlock

class FirEmptyExpressionBlock(
    session: FirSession
) : FirAbstractAnnotatedElement(session, null), FirBlock {
    override val statements = listOf()
}