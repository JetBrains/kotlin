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

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.completion.KotlinCompletionContributor
import org.jetbrains.kotlin.idea.completion.LookupElementFactory
import org.jetbrains.kotlin.idea.kdoc.getParamDescriptors
import org.jetbrains.kotlin.idea.kdoc.getResolutionScope
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer

class KDocCompletionContributor(): CompletionContributor() {
    init {
        extend(CompletionType.BASIC, psiElement().inside(javaClass<KDocName>()),
                KDocNameCompletionProvider)

        extend(CompletionType.BASIC,
               psiElement().afterLeaf(
                   StandardPatterns.or(psiElement(KDocTokens.LEADING_ASTERISK), psiElement(KDocTokens.START))),
               KDocTagCompletionProvider)

        extend(CompletionType.BASIC,
               psiElement(KDocTokens.TAG_NAME), KDocTagCompletionProvider)
    }
}

object KDocNameCompletionProvider: CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        KDocNameCompletionSession(parameters, result).complete()
    }
}

class KDocNameCompletionSession(parameters: CompletionParameters,
                                resultSet: CompletionResultSet): CompletionSessionBase(CompletionSessionConfiguration(parameters), parameters, resultSet) {
    override fun doComplete() {
        val position = parameters.getPosition().getParentOfType<KDocName>(false) ?: return
        val declaration = position.getContainingDoc().getOwner() ?: return
        val kdocLink = position.getStrictParentOfType<KDocLink>()!!
        val declarationDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
        if (kdocLink.getTagIfSubject()?.knownTag == KDocKnownTag.PARAM) {
            addParamCompletions(position, declarationDescriptor)
        } else {
            addLinkCompletions(declarationDescriptor)
        }
    }

    private fun addParamCompletions(position: KDocName,
                                    declarationDescriptor: DeclarationDescriptor) {
        val section = position.getContainingSection()
        val documentedParameters = section.findTagsByName("param").map { it.getSubjectName() }.toSet()
        val descriptors = getParamDescriptors(declarationDescriptor)
                .filter { it.getName().asString() !in documentedParameters }

        descriptors.forEach {
            resultSet.addElement(lookupElementFactory.createLookupElement(resolutionFacade, it, false))
        }
    }

    private fun addLinkCompletions(declarationDescriptor: DeclarationDescriptor) {
        val scope = getResolutionScope(resolutionFacade, declarationDescriptor)
        scope.getDescriptors(nameFilter = {name -> prefixMatcher.prefixMatches(name.asString())}).forEach {
            val element = lookupElementFactory.createLookupElement(resolutionFacade, it, false)
            resultSet.addElement(object: LookupElementDecorator<LookupElement>(element) {
                override fun handleInsert(context: InsertionContext?) {
                    // insert only plain name here, no qualifier/parentheses/etc.
                }
            })
        }
    }
}

object KDocTagCompletionProvider: CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val prefix = if (parameters.getPosition().getNode().getElementType() == KDocTokens.TAG_NAME)
            parameters.getPosition().getText().removeSuffix(KotlinCompletionContributor.DEFAULT_DUMMY_IDENTIFIER)
        else
            null
        val resultWithPrefix = if (prefix != null) result.withPrefixMatcher(prefix) else result
        KDocKnownTag.values().forEach {
            resultWithPrefix.addElement(LookupElementBuilder.create("@" + it.name().toLowerCase()))
        }
    }
}
