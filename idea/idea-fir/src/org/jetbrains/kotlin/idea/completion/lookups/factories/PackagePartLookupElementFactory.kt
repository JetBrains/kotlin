/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups.factories

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import org.jetbrains.kotlin.idea.completion.contributors.helpers.insertSymbolAndInvokeCompletion
import org.jetbrains.kotlin.idea.completion.lookups.KotlinLookupObject
import org.jetbrains.kotlin.miniStdLib.letIf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.render

internal class PackagePartLookupElementFactory {
    fun createPackagePartLookupElement(packagePartFqName: FqName): LookupElement {
        val shortName = packagePartFqName.shortName()
        return LookupElementBuilder.create(PackagePartLookupObject(shortName), "${shortName.render()}.")
            .withInsertHandler(PackagePartInsertionHandler)
            .withIcon(AllIcons.Nodes.Package)
            .letIf(!packagePartFqName.parent().isRoot) {
                it.appendTailText("(${packagePartFqName.asString()})", true)
            }
    }
}


private data class PackagePartLookupObject(
    override val shortName: Name,
) : KotlinLookupObject


private object PackagePartInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val lookupElement = item.`object` as PackagePartLookupObject
        val name = lookupElement.shortName.render()
        context.document.replaceString(context.startOffset, context.tailOffset, name)
        context.commitDocument()
        context.insertSymbolAndInvokeCompletion(symbol = ".")
    }
}

