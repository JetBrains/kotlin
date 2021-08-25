/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.types.AbstractTypePreparator
import org.jetbrains.kotlin.types.AbstractTypeRefiner
import org.jetbrains.kotlin.types.TypeCheckerState

fun createIrTypeCheckerState(typeSystemContext: IrTypeSystemContext): TypeCheckerState {
    return TypeCheckerState(
        isErrorTypeEqualsToAnything = false,
        isStubTypeEqualsToAnything = false,
        allowedTypeVariable = false,
        typeSystemContext = typeSystemContext,
        kotlinTypePreparator = AbstractTypePreparator.Default,
        kotlinTypeRefiner = AbstractTypeRefiner.Default
    )
}
