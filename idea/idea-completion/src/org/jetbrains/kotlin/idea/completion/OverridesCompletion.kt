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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.ui.RowIcon
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.completion.handlers.indexOfSkippingSpace
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.moveCaretIntoGeneratedElement
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.generateMember
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class OverridesCompletion(
        private val collector: LookupElementsCollector,
        private val lookupElementFactory: BasicLookupElementFactory
) {
    private val PRESENTATION_RENDERER = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.withOptions {
        modifiers = emptySet()
        includeAdditionalModifiers = false
    }

    fun complete(position: PsiElement, declaration: KtCallableDeclaration?) {
        val isConstructorParameter = position.getNonStrictParentOfType<KtPrimaryConstructor>() != null

        val classOrObject = position.getNonStrictParentOfType<KtClassOrObject>() ?: return

        val members = OverrideMembersHandler(isConstructorParameter).collectMembersToGenerate(classOrObject)

        for (memberObject in members) {
            val descriptor = memberObject.descriptor
            if (declaration != null && !canOverride(descriptor, declaration)) continue
            if (isConstructorParameter && descriptor !is PropertyDescriptor) continue

            var lookupElement = lookupElementFactory.createLookupElement(descriptor)

            var text = "override " + PRESENTATION_RENDERER.render(descriptor)
            if (descriptor is FunctionDescriptor) {
                text += " {...}"
            }

            val baseClass = descriptor.containingDeclaration as ClassDescriptor
            val baseClassName = baseClass.name.asString()

            val baseIcon = (lookupElement.`object` as DeclarationLookupObject).getIcon(0)
            val isImplement = descriptor.modality == Modality.ABSTRACT
            val additionalIcon = if (isImplement)
                AllIcons.Gutter.ImplementingMethod
            else
                AllIcons.Gutter.OverridingMethod
            val icon = RowIcon(baseIcon, additionalIcon)

            val baseClassDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(position.project, baseClass)
            val baseClassIcon = KotlinDescriptorIconProvider.getIcon(baseClass, baseClassDeclaration, 0)

            lookupElement = object : LookupElementDecorator<LookupElement>(lookupElement) {
                override fun getLookupString() = if (declaration == null) "override" else delegate.lookupString // don't use "override" as lookup string when already in the name of declaration
                override fun getAllLookupStrings() = setOf(lookupString, delegate.lookupString)

                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)

                    presentation.itemText = text
                    presentation.isItemTextBold = isImplement
                    presentation.icon = icon
                    presentation.clearTail()
                    presentation.setTypeText(baseClassName, baseClassIcon)
                }

                override fun handleInsert(context: InsertionContext) {
                    val dummyMemberHead = when {
                        declaration != null -> ""
                        isConstructorParameter -> "override val "
                        else -> "override fun "
                    }
                    val dummyMemberTail = when {
                        isConstructorParameter || declaration is KtProperty -> "dummy: Dummy ,@"
                        else -> "dummy() {}"
                    }
                    val dummyMemberText = dummyMemberHead + dummyMemberTail
                    context.document.replaceString(context.startOffset, context.tailOffset, dummyMemberText)

                    val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
                    psiDocumentManager.commitAllDocuments()

                    val dummyMember = context.file.findElementAt(context.startOffset)!!.getStrictParentOfType<KtNamedDeclaration>()!!

                    // keep original modifiers
                    val modifierList = KtPsiFactory(context.project).createModifierList(dummyMember.modifierList!!.text)

                    val prototype = memberObject.generateMember(classOrObject, false)
                    prototype.modifierList!!.replace(modifierList)
                    val insertedMember = dummyMember.replaced(prototype)

                    ShortenReferences.DEFAULT.process(insertedMember)

                    if (isConstructorParameter) {
                        psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)

                        val offset = insertedMember.endOffset
                        val chars = context.document.charsSequence
                        val commaOffset = chars.indexOfSkippingSpace(',', offset)!!
                        val atCharOffset = chars.indexOfSkippingSpace('@', commaOffset + 1)!!
                        context.document.deleteString(offset, atCharOffset + 1)

                        context.editor.moveCaret(offset)
                    }
                    else {
                        moveCaretIntoGeneratedElement(context.editor, insertedMember)
                    }
                }
            }

            lookupElement.assignPriority(if (isImplement) ItemPriority.IMPLEMENT else ItemPriority.OVERRIDE)

            collector.addElement(lookupElement)
        }
    }

    private fun canOverride(descriptorToOverride: CallableMemberDescriptor, declaration: KtCallableDeclaration): Boolean {
        when (declaration) {
            is KtFunction -> return descriptorToOverride is FunctionDescriptor

            is KtValVarKeywordOwner -> {
                if (descriptorToOverride !is PropertyDescriptor) return false
                return if (declaration.valOrVarKeyword?.node?.elementType == KtTokens.VAL_KEYWORD) {
                    !descriptorToOverride.isVar
                }
                else {
                    true // var can override either var or val
                }
            }

            else -> return false
        }
    }
}