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

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.completion.isAfterDot
import org.jetbrains.kotlin.idea.core.completion.DeclarationDescriptorLookupObject
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.DescriptorUtils

abstract class KotlinCallableInsertHandler : BaseDeclarationInsertHandler() {
    public override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)

        addImport(context, item)
    }

    private fun addImport(context : InsertionContext, item : LookupElement) {
        PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments()

        val file = context.getFile()
        val o = item.getObject()
        if (file is JetFile && o is DeclarationDescriptorLookupObject) {
            val descriptor = o.descriptor as? CallableDescriptor
            if (descriptor != null) {
                // for completion after dot, import insertion may be required only for extensions
                if (context.isAfterDot() && descriptor.getExtensionReceiverParameter() == null) {
                    return
                }

                if (DescriptorUtils.isTopLevelDeclaration(descriptor)) {
                    runWriteAction {
                        ImportInsertHelper.getInstance(context.getProject()).importDescriptor(file, descriptor)
                    }
                }
            }
        }
    }
}

