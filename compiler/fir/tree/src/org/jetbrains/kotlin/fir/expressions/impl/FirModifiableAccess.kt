/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirAccess

interface FirModifiableAccess : FirAccess {
    override var safe: Boolean
        get() = super.safe
        set(_) {}

    override var explicitReceiver: FirExpression?
        get() = super.explicitReceiver
        set(_) {}
}