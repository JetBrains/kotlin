/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.types.ConeKotlinType

abstract class Flow {
    abstract val approvedTypeStatements: TypeStatements
    abstract fun unwrapVariable(variable: RealVariable): RealVariable
    abstract fun getType(variable: RealVariable): Set<ConeKotlinType>?
}
