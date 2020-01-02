/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage

import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.psi.KtElement

abstract class CreateFromUsageFixBase<T : KtElement>(element: T) : KotlinQuickFixAction<T>(element) {
    override fun getFamilyName(): String = KotlinBundle.message("create.from.usage.family")
}
