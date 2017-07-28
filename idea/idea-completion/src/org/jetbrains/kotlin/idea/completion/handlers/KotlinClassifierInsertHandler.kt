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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.completion.isAfterDot
import org.jetbrains.kotlin.idea.completion.isArtificialImportAliasedDescriptor
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

object KotlinClassifierInsertHandler : BaseDeclarationInsertHandler() {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        surroundWithBracesIfInStringTemplate(context)

        super.handleInsert(context, item)

        val file = context.file
        if (file is KtFile) {
            if (!context.isAfterDot()) {
                val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
                psiDocumentManager.commitAllDocuments()

                val startOffset = context.startOffset
                val document = context.document

                val lookupObject = item.`object` as DeclarationLookupObject
                if (lookupObject.descriptor?.isArtificialImportAliasedDescriptor == true) return // never need to insert import or use qualified name for import-aliased class
                
                val qualifiedName = qualifiedName(lookupObject)

                // first try to resolve short name for faster handling
                val token = file.findElementAt(startOffset)!!
                val nameRef = token.parent as? KtNameReferenceExpression
                if (nameRef != null) {
                    val bindingContext = nameRef.analyze(BodyResolveMode.PARTIAL)
                    val target = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, nameRef]
                                 ?: bindingContext[BindingContext.REFERENCE_TARGET, nameRef] as? ClassDescriptor
                    if (target != null && IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(target) == qualifiedName) return
                }

                val tempPrefix = if (nameRef != null) {
                    val isAnnotation = CallTypeAndReceiver.detect(nameRef) is CallTypeAndReceiver.ANNOTATION
                    // we insert space so that any preceding spaces inserted by formatter on reference shortening are deleted
                    // (but not for annotations where spaces are not allowed after @)
                    if (isAnnotation) "" else " "
                }
                else {
                    "$;val v:"  // if we have no reference in the current context we have a more complicated prefix to get one
                }
                val tempSuffix = ".xxx" // we add "xxx" after dot because of KT-9606
                document.replaceString(startOffset, context.tailOffset, tempPrefix + qualifiedName + tempSuffix)

                psiDocumentManager.commitAllDocuments()

                val classNameStart = startOffset + tempPrefix.length
                val classNameEnd = classNameStart + qualifiedName.length
                val rangeMarker = document.createRangeMarker(classNameStart, classNameEnd)
                val wholeRangeMarker = document.createRangeMarker(startOffset, classNameEnd + tempSuffix.length)

                ShortenReferences.DEFAULT.process(file, classNameStart, classNameEnd)
                psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

                if (rangeMarker.isValid && wholeRangeMarker.isValid) {
                    document.deleteString(wholeRangeMarker.startOffset, rangeMarker.startOffset)
                    document.deleteString(rangeMarker.endOffset, wholeRangeMarker.endOffset)
                }
            }
        }
    }

    private fun qualifiedName(lookupObject: DeclarationLookupObject): String {
        return if (lookupObject.descriptor != null) {
            IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(lookupObject.descriptor as ClassifierDescriptor)
        }
        else {
            val qualifiedName = (lookupObject.psiElement as PsiClass).qualifiedName!!
            if (FqNameUnsafe.isValid(qualifiedName)) FqNameUnsafe(qualifiedName).render() else qualifiedName
        }
    }
}
