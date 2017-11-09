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
import org.jetbrains.kotlin.idea.completion.isArtificialImportAliasedDescriptor
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.DescriptorUtils

abstract class KotlinCallableInsertHandler(val callType: CallType<*>) : BaseDeclarationInsertHandler() {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)

        addImport(context, item)
    }

    private fun addImport(context : InsertionContext, item : LookupElement) {
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitAllDocuments()

        val file = context.file
        val o = item.`object`
        if (file is KtFile && o is DeclarationLookupObject) {
            val descriptor = o.descriptor as? CallableDescriptor ?: return
            if (descriptor.extensionReceiverParameter != null || callType == CallType.CALLABLE_REFERENCE) {
                if (DescriptorUtils.isTopLevelDeclaration(descriptor) && !descriptor.isArtificialImportAliasedDescriptor) {
                    ImportInsertHelper.getInstance(context.project).importDescriptor(file, descriptor)
                }
            }
            else if (callType == CallType.DEFAULT) {
                if (descriptor.isArtificialImportAliasedDescriptor) return
                val fqName = descriptor.importableFqName ?: return
                context.document.replaceString(context.startOffset, context.tailOffset, fqName.render() + " ") // insert space after for correct parsing

                psiDocumentManager.commitAllDocuments()

                ShortenReferences.DEFAULT.process(file, context.startOffset, context.tailOffset - 1)

                psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)

                // delete space
                if (context.document.isTextAt(context.tailOffset - 1, " ")) { // sometimes space can be lost because of reformatting
                    context.document.deleteString(context.tailOffset - 1, context.tailOffset)
                }
            }
        }
    }
}

