/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.fir

import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.AbstractAddAccessorsIntention
import org.jetbrains.kotlin.psi.KtProperty

class AddAccessorsIntention(
    addGetter: Boolean, addSetter: Boolean,
    private val priority: PriorityAction.Priority
) :
    AbstractAddAccessorsIntention(addGetter, addSetter), PriorityAction {
    // FE1.0 logic in org.jetbrains.kotlin.idea.intentions.AddAccessorsIntention has additional defensive checks to ensure the
    // intention only shows up if the property in question indeed needs initialization or accessors. Those checks are unnecessary
    // since this factory is coupled with MUST_BE_INITIALIZED_OR_BE_ABSTRACT diagnostic, which has already checked the applicability
    // of this intention.
    override fun applicabilityRange(element: KtProperty): TextRange? = element.textRange

    override fun getPriority(): PriorityAction.Priority = priority
}