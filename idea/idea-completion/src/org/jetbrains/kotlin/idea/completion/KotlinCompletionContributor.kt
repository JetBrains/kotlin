/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.Key
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns.elementType
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.PsiComment
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletion
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletionSession
import org.jetbrains.kotlin.idea.completion.stringTemplates.InsertStringTemplateBracesLookupElementDecorator
import org.jetbrains.kotlin.idea.completion.stringTemplates.StringTemplateCompletion
import org.jetbrains.kotlin.idea.statistics.CompletionFUSCollector.completionStatsData
import org.jetbrains.kotlin.idea.statistics.CompletionTypeStats
import org.jetbrains.kotlin.idea.statistics.FileTypeStats
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import kotlin.math.max

var KtFile.doNotComplete: Boolean? by UserDataProperty(Key.create("DO_NOT_COMPLETE"))

class KotlinCompletionContributor : CompletionContributor() {
    private val AFTER_NUMBER_LITERAL = psiElement().afterLeafSkipping(
        psiElement().withText(""),
        psiElement().withElementType(elementType().oneOf(KtTokens.FLOAT_LITERAL, KtTokens.INTEGER_LITERAL))
    )
    private val AFTER_INTEGER_LITERAL_AND_DOT = psiElement().afterLeafSkipping(
        psiElement().withText("."),
        psiElement().withElementType(elementType().oneOf(KtTokens.INTEGER_LITERAL))
    )

    companion object {
        val DEFAULT_DUMMY_IDENTIFIER: String =
            CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$" // add '$' to ignore context after the caret
    }

    init {
        val provider = object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                performCompletion(parameters, result)
            }
        }
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), provider)
        extend(CompletionType.SMART, PlatformPatterns.psiElement(), provider)
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val offset = context.startOffset
        val psiFile = context.file
        val tokenBefore = psiFile.findElementAt(max(0, offset - 1))

        // this code will make replacement offset "modified" and prevents altering it by the code in CompletionProgressIndicator
        context.replacementOffset = context.replacementOffset

        val dummyIdentifierCorrected = service<CompletionDummyIdentifierProviderService>().correctPositionForStringTemplateEntry(context)
        if (dummyIdentifierCorrected) {
            return
        }
        context.dummyIdentifier = when {
            context.completionType == CompletionType.SMART -> DEFAULT_DUMMY_IDENTIFIER

            PackageDirectiveCompletion.ACTIVATION_PATTERN.accepts(tokenBefore) -> PackageDirectiveCompletion.DUMMY_IDENTIFIER

            else -> service<CompletionDummyIdentifierProviderService>().provideDummyIdentifier(context)
        }

        val tokenAt = psiFile.findElementAt(max(0, offset))
        if (tokenAt != null) {
            /* do not use parent expression if we are at the end of line - it's probably parsed incorrectly */
            if (context.completionType == CompletionType.SMART && !isAtEndOfLine(offset, context.editor.document)) {
                var parent = tokenAt.parent
                if (parent is KtExpression && parent !is KtBlockExpression) {
                    // search expression to be replaced - go up while we are the first child of parent expression
                    var expression: KtExpression = parent
                    parent = expression.parent
                    while (parent is KtExpression && parent.getFirstChild() == expression) {
                        expression = parent
                        parent = expression.parent
                    }

                    val suggestedReplacementOffset = replacementOffsetByExpression(expression)
                    if (suggestedReplacementOffset > context.replacementOffset) {
                        context.replacementOffset = suggestedReplacementOffset
                    }

                    context.offsetMap.addOffset(SmartCompletion.OLD_ARGUMENTS_REPLACEMENT_OFFSET, expression.endOffset)

                    val argumentList = (expression.parent as? KtValueArgument)?.parent as? KtValueArgumentList
                    if (argumentList != null) {
                        context.offsetMap.addOffset(
                            SmartCompletion.MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET,
                            argumentList.rightParenthesis?.textRange?.startOffset ?: argumentList.endOffset
                        )
                    }
                }
            }

            // IDENTIFIER when 'f<caret>oo: Foo'
            // COLON when 'foo<caret>: Foo'
            if (tokenAt.node.elementType == KtTokens.IDENTIFIER || tokenAt.node.elementType == KtTokens.COLON) {
                val parameter = tokenAt.parent as? KtParameter
                if (parameter != null) {
                    context.offsetMap.addOffset(VariableOrParameterNameWithTypeCompletion.REPLACEMENT_OFFSET, parameter.endOffset)
                }
            }
        }

    }

    private fun replacementOffsetByExpression(expression: KtExpression): Int {
        when (expression) {
            is KtCallExpression -> {
                val calleeExpression = expression.calleeExpression
                if (calleeExpression != null) {
                    return calleeExpression.textRange!!.endOffset
                }
            }

            is KtQualifiedExpression -> {
                val selector = expression.selectorExpression
                if (selector != null) {
                    return replacementOffsetByExpression(selector)
                }
            }
        }
        return expression.textRange!!.endOffset
    }


    private fun performCompletion(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        val parametersOriginFile = parameters.originalFile
        if (position.containingFile !is KtFile || parametersOriginFile !is KtFile) return
        if (parametersOriginFile.doNotComplete == true) return

        val toFromOriginalFileMapper = ToFromOriginalFileMapper.create(parameters)

        StringTemplateCompletion.correctParametersForInStringTemplateCompletion(parameters)?.let { correctedParameters ->
            doComplete(correctedParameters, toFromOriginalFileMapper, result,
                       lookupElementPostProcessor = { InsertStringTemplateBracesLookupElementDecorator(it) })
            return
        }

        doComplete(parameters, toFromOriginalFileMapper, result)
    }

    private fun doComplete(
        parameters: CompletionParameters,
        toFromOriginalFileMapper: ToFromOriginalFileMapper,
        result: CompletionResultSet,
        lookupElementPostProcessor: ((LookupElement) -> LookupElement)? = null
    ) {
        val name = parameters.originalFile.virtualFile?.name ?: "default.kts"
        completionStatsData = completionStatsData?.copy(
            completionType = when (parameters.completionType) {
                CompletionType.BASIC -> CompletionTypeStats.BASIC
                CompletionType.CLASS_NAME -> CompletionTypeStats.BASIC //there is no class name anymore actually
                CompletionType.SMART -> CompletionTypeStats.SMART
            },
            fileType = FileTypeStats.parseFromFileName(name),
            invocationCount = parameters.invocationCount
        )
        val position = parameters.position
        if (position.getNonStrictParentOfType<PsiComment>() != null) {
            // don't stop here, allow other contributors to run
            return
        }

        if (shouldSuppressCompletion(parameters, result.prefixMatcher)) {
            result.stopHere()
            return
        }

        if (PackageDirectiveCompletion.perform(parameters, result)) {
            result.stopHere()
            return
        }

        for (extension in KotlinCompletionExtension.EP_NAME.getExtensions()) {
            if (extension.perform(parameters, result)) return
        }

        fun addPostProcessor(session: CompletionSession) {
            if (lookupElementPostProcessor != null) {
                session.addLookupElementPostProcessor(lookupElementPostProcessor)
            }
        }

        result.restartCompletionWhenNothingMatches()

        val configuration = CompletionSessionConfiguration(parameters)
        if (parameters.completionType == CompletionType.BASIC) {
            val session = BasicCompletionSession(configuration, parameters, toFromOriginalFileMapper, result)

            addPostProcessor(session)

            if (parameters.isAutoPopup && session.shouldDisableAutoPopup()) {
                result.stopHere()
                return
            }

            val somethingAdded = session.complete()
            if (!somethingAdded && parameters.invocationCount < 2) {
                // Rerun completion if nothing was found
                val newConfiguration = CompletionSessionConfiguration(
                    useBetterPrefixMatcherForNonImportedClasses = false,
                    nonAccessibleDeclarations = false,
                    javaGettersAndSetters = true,
                    javaClassesNotToBeUsed = false,
                    staticMembers = parameters.invocationCount > 0,
                    dataClassComponentFunctions = true
                )

                val newSession = BasicCompletionSession(newConfiguration, parameters, toFromOriginalFileMapper, result)
                addPostProcessor(newSession)
                newSession.complete()
            }
        } else {
            val session = SmartCompletionSession(configuration, parameters, toFromOriginalFileMapper, result)
            addPostProcessor(session)
            session.complete()
        }
    }

    private fun shouldSuppressCompletion(parameters: CompletionParameters, prefixMatcher: PrefixMatcher): Boolean {
        val position = parameters.position
        val invocationCount = parameters.invocationCount

        // no completion inside number literals
        if (AFTER_NUMBER_LITERAL.accepts(position)) return true

        // no completion auto-popup after integer and dot
        if (invocationCount == 0 && prefixMatcher.prefix.isEmpty() && AFTER_INTEGER_LITERAL_AND_DOT.accepts(position)) return true

        return false
    }

    private fun isAtEndOfLine(offset: Int, document: Document): Boolean {
        var i = offset
        val chars = document.charsSequence
        while (i < chars.length) {
            val c = chars[i]
            if (c == '\n') return true
            if (!Character.isWhitespace(c)) return false
            i++
        }
        return true
    }
}

abstract class KotlinCompletionExtension {
    abstract fun perform(parameters: CompletionParameters, result: CompletionResultSet): Boolean

    companion object {
        val EP_NAME: ExtensionPointName<KotlinCompletionExtension> =
            ExtensionPointName.create<KotlinCompletionExtension>("org.jetbrains.kotlin.completionExtension")
    }
}
