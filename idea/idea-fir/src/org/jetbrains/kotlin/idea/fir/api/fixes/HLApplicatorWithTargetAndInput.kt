/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.fixes

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicator
import org.jetbrains.kotlin.idea.fir.api.applicator.HLApplicatorInput

class HLApplicatorWithTargetAndInput<PSI : PsiElement, INPUT : HLApplicatorInput>(
    val applicator: HLApplicator<PSI, INPUT>,
    private val targetAndInput: HLApplicatorTargetWithInput<PSI, INPUT>,
) {
    operator fun component1() = applicator
    operator fun component2() = targetAndInput.target
    operator fun component3() = targetAndInput.input
}
