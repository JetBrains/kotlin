/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.core.completion.DeclarationDescriptorLookupObject
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers

open class BaseDeclarationInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val descriptor = (item.getObject() as? DeclarationDescriptorLookupObject)?.descriptor
        if (descriptor != null) {
            val name = descriptor.getName()
            val nameInCode = IdeDescriptorRenderers.SOURCE_CODE.renderName(name)
            val document = context.getDocument()
            val needEscaping = nameInCode != name.asString()
            // we check that text inserted matches the name because something else can be inserted by custom insert handler
            if (needEscaping && document.getText(TextRange(context.getStartOffset(), context.getTailOffset())) == name.asString()) {
                document.replaceString(context.getStartOffset(), context.getTailOffset(), nameInCode)
            }
        }
    }
}
