/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.extended.RedundantExplicitTypeChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.RedundantModalityModifierChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extended.RedundantReturnUnitType
import org.jetbrains.kotlin.fir.analysis.checkers.extended.RedundantVisibilityModifierChecker

object ExtendedDeclarationCheckers : DeclarationCheckers() {
    override val declarationCheckers = listOf(
        RedundantVisibilityModifierChecker,
        RedundantReturnUnitType
    )

    override val memberDeclarationCheckers = listOf(
        RedundantModalityModifierChecker,
        RedundantExplicitTypeChecker
    )

}
