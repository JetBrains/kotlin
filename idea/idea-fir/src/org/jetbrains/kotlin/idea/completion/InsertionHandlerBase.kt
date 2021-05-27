/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.idea.completion.lookups.KotlinLookupObject
import org.jetbrains.kotlin.psi.KtFile
import kotlin.reflect.KClass

internal abstract class InsertionHandlerBase<LO : KotlinLookupObject>(
    private val lookupObjectClass: KClass<LO>
) : InsertHandler<LookupElement> {

    final override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val ktFile = context.file as? KtFile ?: return
        val lookupObject = item.`object`
        check(lookupObjectClass.isInstance(lookupObject))

        @Suppress("UNCHECKED_CAST")
        handleInsert(context, item, ktFile, lookupObject as LO)
    }

    abstract fun handleInsert(context: InsertionContext, item: LookupElement, ktFile: KtFile, lookupObject: LO)
}