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
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Key
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns.elementType
import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.*
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.performCompletionWithOutOfBlockTracking
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletion
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletionSession
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public var KtFile.doNotComplete: Boolean? by UserDataProperty(Key.create("DO_NOT_COMPLETE"))

public class KotlinCompletionContributor : CompletionContributor() {
    private val AFTER_NUMBER_LITERAL = psiElement().afterLeafSkipping(psiElement().withText(""), psiElement().withElementType(elementType().oneOf(KtTokens.FLOAT_LITERAL, KtTokens.INTEGER_LITERAL)))
    private val AFTER_INTEGER_LITERAL_AND_DOT = psiElement().afterLeafSkipping(psiElement().withText("."), psiElement().withElementType(elementType().oneOf(KtTokens.INTEGER_LITERAL)))

    companion object {
        public val DEFAULT_DUMMY_IDENTIFIER: String = CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "$" // add '$' to ignore context after the caret

        private val STRING_TEMPLATE_AFTER_DOT_REAL_START_OFFSET = OffsetKey.create("STRING_TEMPLATE_AFTER_DOT_REAL_START_OFFSET")
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
        val psiFile = context.getFile()
        if (psiFile !is KtFile) return

        // this code will make replacement offset "modified" and prevents altering it by the code in CompletionProgressIndicator
        context.replacementOffset = context.replacementOffset

        val offset = context.getStartOffset()
        val tokenBefore = psiFile.findElementAt(Math.max(0, offset - 1))

        if (offset > 0 && tokenBefore!!.node.elementType == KtTokens.REGULAR_STRING_PART && tokenBefore.text.startsWith(".")) {
            val prev = tokenBefore.parent.prevSibling
            if (prev != null && prev is KtSimpleNameStringTemplateEntry) {
                val expression = prev.expression
                if (expression != null) {
                    val prefix = tokenBefore.text.substring(0, offset - tokenBefore.startOffset)
                    context.dummyIdentifier = "{" + expression.text + prefix + CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "}"
                    context.offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, expression.startOffset)
                    context.offsetMap.addOffset(STRING_TEMPLATE_AFTER_DOT_REAL_START_OFFSET, offset + 1)
                    return
                }
            }
        }

        context.dummyIdentifier = when {
            context.getCompletionType() == CompletionType.SMART -> DEFAULT_DUMMY_IDENTIFIER

            PackageDirectiveCompletion.ACTIVATION_PATTERN.accepts(tokenBefore) -> PackageDirectiveCompletion.DUMMY_IDENTIFIER

            isInClassHeader(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER // do not add '$' to not interrupt class declaration parsing

            isInUnclosedSuperQualifier(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + ">"

            isInSimpleStringTemplate(tokenBefore) -> CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED

            else -> specialLambdaSignatureDummyIdentifier(tokenBefore)
                    ?: specialExtensionReceiverDummyIdentifier(tokenBefore)
                    ?: specialInTypeArgsDummyIdentifier(tokenBefore)
                    ?: specialInArgumentListDummyIdentifier(tokenBefore)
                    ?: DEFAULT_DUMMY_IDENTIFIER
        }

        if (context.getCompletionType() == CompletionType.SMART && !isAtEndOfLine(offset, context.getEditor().getDocument()) /* do not use parent expression if we are at the end of line - it's probably parsed incorrectly */) {
            val tokenAt = psiFile.findElementAt(Math.max(0, offset))
            if (tokenAt != null) {
                var parent = tokenAt.getParent()
                if (parent is KtExpression && parent !is KtBlockExpression) {
                    // search expression to be replaced - go up while we are the first child of parent expression
                    var expression: KtExpression = parent
                    parent = expression.getParent()
                    while (parent is KtExpression && parent.getFirstChild() == expression) {
                        expression = parent
                        parent = expression.getParent()
                    }

                    val suggestedReplacementOffset = replacementOffsetByExpression(expression)
                    if (suggestedReplacementOffset > context.getReplacementOffset()) {
                        context.setReplacementOffset(suggestedReplacementOffset)
                    }

                    context.getOffsetMap().addOffset(SmartCompletion.OLD_ARGUMENTS_REPLACEMENT_OFFSET, expression.endOffset)

                    val argumentList = (expression.getParent() as? KtValueArgument)?.getParent() as? KtValueArgumentList
                    if (argumentList != null) {
                        context.getOffsetMap().addOffset(SmartCompletion.MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET,
                                                         argumentList.getRightParenthesis()?.getTextRange()?.getStartOffset() ?: argumentList.endOffset)
                    }
                }
            }
        }
    }

    private fun replacementOffsetByExpression(expression: KtExpression): Int {
        when (expression) {
            is KtCallExpression -> {
                val calleeExpression = expression.getCalleeExpression()
                if (calleeExpression != null) {
                    return calleeExpression.getTextRange()!!.getEndOffset()
                }
            }

            is KtQualifiedExpression -> {
                val selector = expression.getSelectorExpression()
                if (selector != null) {
                    return replacementOffsetByExpression(selector)
                }
            }
        }
        return expression.getTextRange()!!.getEndOffset()
    }

    private fun isInClassHeader(tokenBefore: PsiElement?): Boolean {
        val classOrObject = tokenBefore?.parents?.firstIsInstanceOrNull<KtClassOrObject>() ?: return false
        val name = classOrObject.getNameIdentifier() ?: return false
        val body = classOrObject.getBody() ?: return false
        val offset = tokenBefore!!.startOffset
        return name.endOffset <= offset && offset <= body.startOffset
    }

    private fun specialLambdaSignatureDummyIdentifier(tokenBefore: PsiElement?): String? {
        var leaf = tokenBefore
        while (leaf is PsiWhiteSpace || leaf is PsiComment) {
            leaf = leaf.prevLeaf(true)
        }

        val lambda = leaf?.parents?.firstOrNull { it is KtFunctionLiteral } ?: return null

        val lambdaChild = leaf!!.parents.takeWhile { it != lambda }.lastOrNull() ?: return null
        if (lambdaChild is KtParameterList) return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED

        if (lambdaChild !is KtBlockExpression) return null
        val blockChild = leaf.parents.takeWhile { it != lambdaChild }.lastOrNull()
        if (blockChild !is PsiErrorElement) return null
        val inIncompleteSignature = blockChild.siblings(forward = false, withItself = false).all {
            when (it) {
                is PsiWhiteSpace, is PsiComment -> true
                else -> false
            }
        }
        return if (inIncompleteSignature) CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + "->" else null

    }

    private val declarationKeywords = TokenSet.create(KtTokens.FUN_KEYWORD, KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD)
    private val declarationTokens = TokenSet.orSet(TokenSet.create(KtTokens.IDENTIFIER, KtTokens.LT, KtTokens.GT,
                                                                   KtTokens.COMMA, KtTokens.DOT, KtTokens.QUEST, KtTokens.COLON,
                                                                   KtTokens.IN_KEYWORD, KtTokens.OUT_KEYWORD,
                                                                   KtTokens.LPAR, KtTokens.RPAR, KtTokens.ARROW,
                                                                   TokenType.ERROR_ELEMENT),
                                                   KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET)

    private fun specialExtensionReceiverDummyIdentifier(tokenBefore: PsiElement?): String? {
        var token = tokenBefore ?: return null
        var ltCount = 0
        var gtCount = 0
        val builder = StringBuilder()
        while (true) {
            val tokenType = token.getNode()!!.getElementType()
            if (tokenType in declarationKeywords) {
                val balance = ltCount - gtCount
                if (balance < 0) return null
                builder.append(token.getText()!!.reversed())
                builder.reverse()

                var tail = "X" + ">".repeat(balance) + ".f"
                if (tokenType == KtTokens.FUN_KEYWORD) {
                    tail += "()"
                }
                builder append tail

                val text = builder.toString()
                val file = KtPsiFactory(tokenBefore.getProject()).createFile(text)
                val declaration = file.getDeclarations().singleOrNull() ?: return null
                if (declaration.getTextLength() != text.length()) return null
                val containsErrorElement = !PsiTreeUtil.processElements(file, PsiElementProcessor<PsiElement>{ it !is PsiErrorElement })
                return if (containsErrorElement) null else tail + "$"
            }
            if (tokenType !in declarationTokens) return null
            if (tokenType == KtTokens.LT) ltCount++
            if (tokenType == KtTokens.GT) gtCount++
            builder.append(token.getText()!!.reversed())
            token = PsiTreeUtil.prevLeaf(token) ?: return null
        }
    }

    private fun performCompletion(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        val positionFile = position.containingFile as? KtFile ?: return
        val originalFile = parameters.originalFile as KtFile
        if (originalFile.doNotComplete ?: false) return

        val toFromOriginalFileMapper = ToFromOriginalFileMapper(originalFile, positionFile, parameters.offset)

        if (position.node.elementType == KtTokens.LONG_TEMPLATE_ENTRY_START) {
            val expression = (position.parent as KtBlockStringTemplateEntry).expression as KtDotQualifiedExpression
            val correctedPosition = (expression.selectorExpression as KtNameReferenceExpression).firstChild
            val context = position.getUserData(CompletionContext.COMPLETION_CONTEXT_KEY)!!
            val correctedOffset = context.offsetMap.getOffset(STRING_TEMPLATE_AFTER_DOT_REAL_START_OFFSET)
            val correctedParameters = parameters.withPosition(correctedPosition, correctedOffset)
            performCompletionWithOutOfBlockTracking(position) {
                doComplete(correctedParameters, toFromOriginalFileMapper, result,
                           lookupElementPostProcessor = { wrapLookupElementForStringTemplateAfterDotCompletion(it) })
            }
            return
        }

        performCompletionWithOutOfBlockTracking(position) {
            doComplete(parameters, toFromOriginalFileMapper, result)
        }
    }

    private fun doComplete(
            parameters: CompletionParameters,
            toFromOriginalFileMapper: ToFromOriginalFileMapper,
            result: CompletionResultSet,
            lookupElementPostProcessor: ((LookupElement) -> LookupElement)? = null
    ) {
        val position = parameters.position
        if (position.getNonStrictParentOfType<PsiComment>() != null) {
            // don't stop here, allow other contributors to run
            return
        }

        if (shouldSuppressCompletion(parameters, result.getPrefixMatcher())) {
            result.stopHere()
            return
        }

        if (PackageDirectiveCompletion.perform(parameters, result)) {
            result.stopHere()
            return
        }

        if (PropertyKeyCompletion.perform(parameters, result)) return

        fun addPostProcessor(session: CompletionSession) {
            if (lookupElementPostProcessor != null) {
                session.addLookupElementPostProcessor(lookupElementPostProcessor)
            }
        }

        try {
            result.restartCompletionWhenNothingMatches()

            val configuration = CompletionSessionConfiguration(parameters)
            if (parameters.getCompletionType() == CompletionType.BASIC) {
                val session = BasicCompletionSession(configuration, parameters, toFromOriginalFileMapper, result)

                addPostProcessor(session)

                if (parameters.isAutoPopup() && session.shouldDisableAutoPopup()) {
                    result.stopHere()
                    return
                }

                val somethingAdded = session.complete()
                if (!somethingAdded && parameters.invocationCount < 2) {
                    // Rerun completion if nothing was found
                    val newConfiguration = CompletionSessionConfiguration(
                            completeNonImportedClasses = true,
                            completeNonAccessibleDeclarations = false,
                            filterOutJavaGettersAndSetters = false,
                            completeJavaClassesNotToBeUsed = false,
                            completeStaticMembers = parameters.invocationCount > 0
                    )

                    val newSession = BasicCompletionSession(newConfiguration, parameters, toFromOriginalFileMapper, result)
                    addPostProcessor(newSession)
                    newSession.complete()
                }
            }
            else {
                val session = SmartCompletionSession(configuration, parameters, toFromOriginalFileMapper, result)
                addPostProcessor(session)
                session.complete()
            }
        }
        catch (e: ProcessCanceledException) {
            throw rethrowWithCancelIndicator(e)
        }
    }

    private fun wrapLookupElementForStringTemplateAfterDotCompletion(lookupElement: LookupElement): LookupElement {
        return object : LookupElementDecorator<LookupElement>(lookupElement) {
            override fun handleInsert(context: InsertionContext) {
                val document = context.document
                val startOffset = context.startOffset

                val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
                psiDocumentManager.commitAllDocuments()

                assert(startOffset > 1 && document.charsSequence[startOffset - 1] == '.')
                val token = context.file.findElementAt(startOffset - 2)!!
                assert(token.node.elementType == KtTokens.IDENTIFIER)
                val nameRef = token.parent as KtNameReferenceExpression
                assert(nameRef.parent is KtSimpleNameStringTemplateEntry)

                document.insertString(nameRef.startOffset, "{")

                val tailOffset = context.tailOffset
                document.insertString(tailOffset, "}")
                context.tailOffset = tailOffset

                super.handleInsert(context)
            }
        }
    }

    private fun shouldSuppressCompletion(parameters: CompletionParameters, prefixMatcher: PrefixMatcher): Boolean {
        val position = parameters.getPosition()
        val invocationCount = parameters.getInvocationCount()

        // no completion inside number literals
        if (AFTER_NUMBER_LITERAL.accepts(position)) return true

        // no completion auto-popup after integer and dot
        if (invocationCount == 0 && prefixMatcher.getPrefix().isEmpty() && AFTER_INTEGER_LITERAL_AND_DOT.accepts(position)) return true

        // no auto-popup on typing after "val", "var" and "fun" because it's likely the name of the declaration which is being typed by user
        if (invocationCount == 0) {
            val callable = isInExtensionReceiverOf(position)
            if (callable != null) {
                return when (callable) {
                    is KtNamedFunction -> prefixMatcher.prefix.let { it.isEmpty() || it[0].isLowerCase() /* function name usually starts with lower case letter */ }
                    else -> true
                }
            }
        }

        return false
    }

    private fun isInExtensionReceiverOf(position: PsiElement): KtCallableDeclaration? {
        val nameRef = position.getParent() as? KtNameReferenceExpression ?: return null
        val userType = nameRef.getParent() as? KtUserType ?: return null
        val typeRef = userType.getParent() as? KtTypeReference ?: return null
        if (userType != typeRef.typeElement) return null
        val parent = typeRef.getParent()
        return when (parent) {
            is KtNamedFunction -> parent.check { typeRef == it.receiverTypeReference }
            is KtProperty -> parent.check { typeRef == it.receiverTypeReference }
            else -> null
        }
    }

    private fun isAtEndOfLine(offset: Int, document: Document): Boolean {
        var i = offset
        val chars = document.getCharsSequence()
        while (i < chars.length()) {
            val c = chars.charAt(i)
            if (c == '\n') return true
            if (!Character.isWhitespace(c)) return false
            i++
        }
        return true
    }

    private fun specialInTypeArgsDummyIdentifier(tokenBefore: PsiElement?): String? {
        if (tokenBefore == null) return null

        if (tokenBefore.getParentOfType<KtTypeArgumentList>(true) != null) { // already parsed inside type argument list
            return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED // do not insert '$' to not break type argument list parsing
        }

        val pair = unclosedTypeArgListNameAndBalance(tokenBefore) ?: return null
        val (nameToken, balance) = pair
        assert(balance > 0)

        val nameRef = nameToken.getParent() as? KtNameReferenceExpression ?: return null
        val bindingContext = nameRef.getResolutionFacade().analyze(nameRef, BodyResolveMode.PARTIAL)
        val targets = nameRef.getReferenceTargets(bindingContext)
        if (targets.isNotEmpty() && targets.all { it is FunctionDescriptor || it is ClassDescriptor && it.getKind() == ClassKind.CLASS }) {
            return CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED + ">".repeat(balance) + "$"
        }
        else {
            return null
        }
    }

    private fun unclosedTypeArgListNameAndBalance(tokenBefore: PsiElement): Pair<PsiElement, Int>? {
        val nameToken = findCallNameTokenIfInTypeArgs(tokenBefore) ?: return null
        val pair = unclosedTypeArgListNameAndBalance(nameToken)
        if (pair == null) {
            return Pair(nameToken, 1)
        }
        else {
            return Pair(pair.first, pair.second + 1)
        }
    }

    private val callTypeArgsTokens = TokenSet.orSet(TokenSet.create(KtTokens.IDENTIFIER, KtTokens.LT, KtTokens.GT,
                                                                   KtTokens.COMMA, KtTokens.DOT, KtTokens.QUEST, KtTokens.COLON,
                                                                   KtTokens.LPAR, KtTokens.RPAR, KtTokens.ARROW),
                                                   KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET)

    // if the leaf could be located inside type argument list of a call (if parsed properly)
    // then it returns the call name reference this type argument list would belong to
    private fun findCallNameTokenIfInTypeArgs(leaf: PsiElement): PsiElement? {
        var current = leaf
        while (true) {
            val tokenType = current.getNode()!!.getElementType()
            if (tokenType !in callTypeArgsTokens) return null

            if (tokenType == KtTokens.LT) {
                val nameToken = current.prevLeaf(skipEmptyElements = true) ?: return null
                if (nameToken.getNode()!!.getElementType() != KtTokens.IDENTIFIER) return null
                return nameToken
            }

            if (tokenType == KtTokens.GT) { // pass nested type argument list
                val prev = current.prevLeaf(skipEmptyElements = true) ?: return null
                val typeRef = findCallNameTokenIfInTypeArgs(prev) ?: return null
                current = typeRef
                continue
            }

            current = current.prevLeaf(skipEmptyElements = true) ?: return null
        }
    }

    private fun specialInArgumentListDummyIdentifier(tokenBefore: PsiElement?): String? {
        // If we insert $ in the argument list of a delegation specifier, this will break parsing
        // and the following block will not be attached as a body to the constructor. Therefore
        // we need to use a regular identifier.
        val argumentList = tokenBefore?.getNonStrictParentOfType<KtValueArgumentList>() ?: return null
        if (argumentList.getParent() is KtConstructorDelegationCall) return CompletionUtil.DUMMY_IDENTIFIER_TRIMMED
        return null
    }

    private fun isInUnclosedSuperQualifier(tokenBefore: PsiElement?): Boolean {
        if (tokenBefore == null) return false
        val tokensToSkip = TokenSet.orSet(TokenSet.create(KtTokens.IDENTIFIER, KtTokens.DOT ), KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET)
        val tokens = sequence(tokenBefore) { it.prevLeaf() }
        val ltToken = tokens.firstOrNull { it.node.elementType !in tokensToSkip } ?: return false
        if (ltToken.node.elementType != KtTokens.LT) return false
        val superToken = ltToken.prevLeaf { it !is PsiWhiteSpace && it !is PsiComment }
        return superToken?.node?.elementType == KtTokens.SUPER_KEYWORD
    }

    private fun isInSimpleStringTemplate(tokenBefore: PsiElement?): Boolean {
        return tokenBefore?.parents?.firstIsInstanceOrNull<KtStringTemplateExpression>()?.isPlain() ?: false
    }
}
