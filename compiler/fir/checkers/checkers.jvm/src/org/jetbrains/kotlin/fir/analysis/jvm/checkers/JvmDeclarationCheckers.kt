/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirMemberDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration.FirJvmExternalDeclarationChecker

object JvmDeclarationCheckers : DeclarationCheckers() {
    override val memberDeclarationCheckers: Set<FirMemberDeclarationChecker>
        get() = setOf(
            FirJvmExternalDeclarationChecker,
        )
}
