/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.allowResolveInDispatchThread
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.completion.isAfterDot
import org.jetbrains.kotlin.idea.completion.isArtificialImportAliasedDescriptor
import org.jetbrains.kotlin.idea.completion.shortenReferences
import org.jetbrains.kotlin.idea.core.canAddRootPrefix
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver.Companion.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT
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
                // never need to insert import or use qualified name for import-aliased class
                if (lookupObject.descriptor?.isArtificialImportAliasedDescriptor == true) return

                val qualifiedName = qualifiedName(lookupObject)

                // first try to resolve short name for faster handling
                val token = file.findElementAt(startOffset)!!
                val nameRef = token.parent as? KtNameReferenceExpression
                if (nameRef != null) {
                    val bindingContext = allowResolveInDispatchThread { nameRef.analyze(BodyResolveMode.PARTIAL) }
                    val target = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, nameRef]
                        ?: bindingContext[BindingContext.REFERENCE_TARGET, nameRef] as? ClassDescriptor
                    if (target != null && IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(target) == qualifiedName) return
                }

                val tempPrefix = if (nameRef != null) {
                    val isAnnotation = CallTypeAndReceiver.detect(nameRef) is CallTypeAndReceiver.ANNOTATION
                    // we insert space so that any preceding spaces inserted by formatter on reference shortening are deleted
                    // (but not for annotations where spaces are not allowed after @)
                    if (isAnnotation) "" else " "
                } else {
                    "$;val v:"  // if we have no reference in the current context we have a more complicated prefix to get one
                }
                val tempSuffix = ".xxx" // we add "xxx" after dot because of KT-9606
                val qualifierNameWithRootPrefix = qualifiedName.let {
                    if (FqName(it).canAddRootPrefix())
                        ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE_WITH_DOT + it
                    else
                        it
                }
                document.replaceString(startOffset, context.tailOffset, tempPrefix + qualifierNameWithRootPrefix + tempSuffix)

                psiDocumentManager.commitAllDocuments()

                val classNameStart = startOffset + tempPrefix.length
                val classNameEnd = classNameStart + qualifierNameWithRootPrefix.length
                val rangeMarker = document.createRangeMarker(classNameStart, classNameEnd)
                val wholeRangeMarker = document.createRangeMarker(startOffset, classNameEnd + tempSuffix.length)

                shortenReferences(context, classNameStart, classNameEnd)
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
        } else {
            val qualifiedName = (lookupObject.psiElement as PsiClass).qualifiedName!!
            if (FqNameUnsafe.isValid(qualifiedName)) FqNameUnsafe(qualifiedName).render() else qualifiedName
        }
    }
}
