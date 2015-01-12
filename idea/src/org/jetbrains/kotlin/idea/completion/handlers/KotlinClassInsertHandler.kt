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
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.idea.codeInsight.ShortenReferences
import org.jetbrains.kotlin.idea.completion.DeclarationDescriptorLookupObject
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.psi.JetNameReferenceExpression
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.idea.completion.isAfterDot
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers

public object KotlinClassInsertHandler : BaseDeclarationInsertHandler() {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)

        val file = context.getFile()
        if (file is JetFile) {
            if (!context.isAfterDot()) {
                val psiDocumentManager = PsiDocumentManager.getInstance(context.getProject())
                psiDocumentManager.commitAllDocuments()

                val startOffset = context.getStartOffset()
                val document = context.getDocument()

                val qualifiedName = qualifiedNameToInsert(item)

                // first try to resolve short name for faster handling
                val token = file.findElementAt(startOffset)
                val nameRef = token.getParent() as? JetNameReferenceExpression
                if (nameRef != null) {
                    val bindingContext = nameRef.getResolutionFacade().analyze(nameRef, BodyResolveMode.PARTIAL)
                    val target = bindingContext[BindingContext.REFERENCE_TARGET, nameRef] as? ClassDescriptor
                    if (target != null && IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(target) == qualifiedName) return
                }

                val tempPrefix = if (nameRef != null)
                    " " // insert space so that any preceding spaces inserted by formatter on reference shortening are deleted
                else
                    "$;val v:" // if we have no reference in the current context we have a more complicated prefix to get one
                val tempSuffix = ".xxx" // we add "xxx" after dot because of some bugs in resolve (see KT-5145)
                document.replaceString(startOffset, context.getTailOffset(), tempPrefix + qualifiedName + tempSuffix)

                psiDocumentManager.commitAllDocuments()

                val classNameStart = startOffset + tempPrefix.length()
                val classNameEnd = classNameStart + qualifiedName.length()
                val rangeMarker = document.createRangeMarker(classNameStart, classNameEnd)
                val wholeRangeMarker = document.createRangeMarker(startOffset, classNameEnd + tempSuffix.length())

                ShortenReferences.process(file, classNameStart, classNameEnd)
                psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

                if (rangeMarker.isValid() && wholeRangeMarker.isValid()) {
                    document.deleteString(wholeRangeMarker.getStartOffset(), rangeMarker.getStartOffset())
                    document.deleteString(rangeMarker.getEndOffset(), wholeRangeMarker.getEndOffset())
                }
            }
        }
    }

    private fun qualifiedNameToInsert(item: LookupElement): String {
        val lookupObject = item.getObject()
        return when (lookupObject) {
            is DeclarationDescriptorLookupObject -> IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(lookupObject.descriptor as ClassDescriptor)
            is PsiClass -> lookupObject.getQualifiedName()!!
            else -> error("Unknown object in LookupElement with KotlinClassInsertHandler: $lookupObject")
        }
    }
}
