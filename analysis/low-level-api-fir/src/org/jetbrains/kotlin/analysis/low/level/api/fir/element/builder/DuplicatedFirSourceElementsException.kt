/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder

import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.psi.KtElement

class DuplicatedFirSourceElementsException(
    existingFir: FirElement,
    newFir: FirElement,
    psi: KtElement
) : IllegalStateException() {
    override val message: String? = """|The PSI element should be used only once as a real PSI source of FirElement,
       |the elements ${if (existingFir.source === newFir.source) "HAVE" else "DON'T HAVE"} the same instances of source elements 
       |
       |existing FIR element is $existingFir with text:
       |${existingFir.render().trim()}
       |
       |new FIR element is $newFir with text:
       | ${newFir.render().trim()}
       |
       |PSI element is $psi with text in context:
       |${psi.getElementTextWithContext()}""".trimMargin()


    companion object {
        // The are some cases which are still generates FIR elements with duplicated source elements
        // Then such case is met, it's better to be fixed
        // but exception reporting can be easily disabled by setting this to false
        var IS_ENABLED = false
    }
}