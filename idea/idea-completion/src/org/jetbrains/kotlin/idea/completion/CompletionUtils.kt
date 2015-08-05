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
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Key
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.JetIcons
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.completion.handlers.CastReceiverInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.findLabelAndCall
import org.jetbrains.kotlin.idea.util.getImplicitReceiversWithInstanceToExpression
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.nullability
import java.util.ArrayList

enum class ItemPriority {
    DEFAULT,
    BACKING_FIELD,
    NAMED_PARAMETER
}

val ITEM_PRIORITY_KEY = Key<ItemPriority>("ITEM_PRIORITY_KEY")

fun LookupElement.assignPriority(priority: ItemPriority): LookupElement {
    putUserData(ITEM_PRIORITY_KEY, priority)
    return this
}

fun LookupElement.suppressAutoInsertion() = AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(this)

fun LookupElement.withReceiverCast(): LookupElement {
    return object: LookupElementDecorator<LookupElement>(this) {
        override fun handleInsert(context: InsertionContext) {
            super.handleInsert(context)
            CastReceiverInsertHandler.handleInsert(context, getDelegate())
        }
    }
}

fun LookupElement.withBracesSurrounding(): LookupElement {
    return object: LookupElementDecorator<LookupElement>(this) {
        override fun handleInsert(context: InsertionContext) {
            val startOffset = context.getStartOffset()
            context.getDocument().insertString(startOffset, "{")
            context.getOffsetMap().addOffset(CompletionInitializationContext.START_OFFSET, startOffset + 1)

            val tailOffset = context.getTailOffset()
            context.getDocument().insertString(tailOffset, "}")
            context.setTailOffset(tailOffset)

            super.handleInsert(context)
        }
    }
}

val KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY = Key<Unit>("KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY")

fun LookupElement.keepOldArgumentListOnTab(): LookupElement {
    putUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY, Unit)
    return this
}

fun rethrowWithCancelIndicator(exception: ProcessCanceledException): ProcessCanceledException {
    val indicator = CompletionService.getCompletionService().getCurrentCompletion() as CompletionProgressIndicator

    // Force cancel to avoid deadlock in CompletionThreading.delegateWeighing()
    if (!indicator.isCanceled()) {
        indicator.cancel()
    }

    return exception
}

fun PrefixMatcher.asNameFilter() = { name: Name ->
    if (name.isSpecial()) {
        false
    }
    else {
        val identifier = name.getIdentifier()
        if (getPrefix().startsWith("$")) { // we need properties from scope for backing field completion
            prefixMatches("$" + identifier)
        }
        else {
            prefixMatches(identifier)
        }
    }
}

fun LookupElementPresentation.prependTailText(text: String, grayed: Boolean) {
    val tails = getTailFragments()
    clearTail()
    appendTailText(text, grayed)
    tails.forEach { appendTailText(it.text, it.isGrayed()) }
}

enum class CallableWeight {
    local, // local non-extension
    thisClassMember,
    baseClassMember,
    thisTypeExtension,
    baseTypeExtension,
    global, // global non-extension
    notApplicableReceiverNullable
}

val CALLABLE_WEIGHT_KEY = Key<CallableWeight>("CALLABLE_WEIGHT_KEY")

fun InsertionContext.isAfterDot(): Boolean {
    var offset = getStartOffset()
    val chars = getDocument().getCharsSequence()
    while (offset > 0) {
        offset--
        val c = chars.charAt(offset)
        if (!Character.isWhitespace(c)) {
            return c == '.'
        }
    }
    return false
}

// do not complete this items by prefix like "is"
fun shouldCompleteThisItems(prefixMatcher: PrefixMatcher): Boolean {
    val prefix = prefixMatcher.getPrefix()
    val s = "this@"
    return prefix.startsWith(s) || s.startsWith(prefix)
}

class ThisItemLookupObject(val receiverParameter: ReceiverParameterDescriptor, val labelName: Name?) : KeywordLookupObject()

fun ThisItemLookupObject.createLookupElement() = createKeywordWithLabelElement("this", labelName, lookupObject = this)
        .withTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(receiverParameter.type))

fun thisExpressionItems(bindingContext: BindingContext, position: JetExpression, prefix: String): Collection<ThisItemLookupObject> {
    val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, position] ?: return listOf()

    val psiFactory = JetPsiFactory(position)

    val result = ArrayList<ThisItemLookupObject>()
    for ((receiver, expressionFactory) in scope.getImplicitReceiversWithInstanceToExpression()) {
        if (expressionFactory == null) continue
        // if prefix does not start with "this@" do not include immediate this in the form with label
        val expression = expressionFactory.createExpression(psiFactory, shortThis = !prefix.startsWith("this@")) as? JetThisExpression ?: continue
        result.add(ThisItemLookupObject(receiver, expression.getLabelNameAsName()))
    }
    return result
}

fun returnExpressionItems(bindingContext: BindingContext, position: JetElement): Collection<LookupElement> {
    val result = ArrayList<LookupElement>()
    for (parent in position.parentsWithSelf) {
        if (parent is JetDeclarationWithBody) {
            val returnType = parent.returnType(bindingContext)
            val isUnit = returnType == null || KotlinBuiltIns.isUnit(returnType)
            if (parent is JetFunctionLiteral) {
                val (label, call) = parent.findLabelAndCall()
                if (label != null) {
                    result.add(createKeywordWithLabelElement("return", label, addSpace = !isUnit))
                }

                // check if the current function literal is inlined and stop processing outer declarations if it's not
                val callee = call?.getCalleeExpression() as? JetReferenceExpression ?: break // not inlined
                if (!InlineUtil.isInline(bindingContext[BindingContext.REFERENCE_TARGET, callee])) break // not inlined
            }
            else {
                if (parent.hasBlockBody()) {
                    result.add(createKeywordWithLabelElement("return", null, addSpace = !isUnit))

                    if (returnType != null && returnType.nullability() == TypeNullability.NULLABLE) {
                        result.add(createKeywordWithLabelElement("return null", null, addSpace = false))
                    }
                    if (returnType != null && KotlinBuiltIns.isBoolean(returnType.makeNotNullable())) {
                        result.add(createKeywordWithLabelElement("return true", null, addSpace = false))
                        result.add(createKeywordWithLabelElement("return false", null, addSpace = false))
                    }
                }
                break
            }
        }
    }
    return result
}

private fun JetDeclarationWithBody.returnType(bindingContext: BindingContext): JetType? {
    val callable = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? CallableDescriptor ?: return null
    return callable.getReturnType()
}

private fun createKeywordWithLabelElement(keyword: String, label: Name?, addSpace: Boolean, lookupObject: KeywordLookupObject = KeywordLookupObject()): LookupElement {
    val element = createKeywordWithLabelElement(keyword, label, lookupObject)
    return if (addSpace) {
        object: LookupElementDecorator<LookupElement>(element) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.spaceTail().handleInsert(context, getDelegate())
            }
        }
    }
    else {
        element
    }
}

private fun createKeywordWithLabelElement(keyword: String, label: Name?, lookupObject: KeywordLookupObject = KeywordLookupObject()): LookupElementBuilder {
    val labelInCode = label?.render()
    var element = LookupElementBuilder.create(lookupObject, if (label == null) keyword else "$keyword@$labelInCode")
    element = element.withPresentableText(keyword)
    element = element.withBoldness(true)
    if (label != null) {
        element = element.withTailText("@$labelInCode", false)
    }
    return element
}

fun breakOrContinueExpressionItems(position: JetElement, breakOrContinue: String): Collection<LookupElement> {
    val result = ArrayList<LookupElement>()

    parentsLoop@
    for (parent in position.parentsWithSelf) {
        when (parent) {
            is JetLoopExpression -> {
                if (result.isEmpty()) {
                    result.add(createKeywordWithLabelElement(breakOrContinue, null))
                }

                val label = (parent.getParent() as? JetLabeledExpression)?.getLabelNameAsName()
                if (label != null) {
                    result.add(createKeywordWithLabelElement(breakOrContinue, label))
                }
            }

            is JetDeclarationWithBody -> break@parentsLoop //TODO: support non-local break's&continue's when they are supported by compiler
        }
    }
    return result
}

fun LookupElementFactory.createBackingFieldLookupElement(
        property: PropertyDescriptor,
        inDescriptor: DeclarationDescriptor,
        resolutionFacade: ResolutionFacade
): LookupElement? {
    val insideAccessor = inDescriptor is PropertyAccessorDescriptor && inDescriptor.getCorrespondingProperty() == property
    if (!insideAccessor) {
        val container = property.getContainingDeclaration()
        if (container !is ClassDescriptor || !DescriptorUtils.isAncestor(container, inDescriptor, false)) return null // backing field not accessible
    }

    val declaration = (DescriptorToSourceUtils.descriptorToDeclaration(property) as? JetProperty) ?: return null

    val accessors = declaration.getAccessors()
    if (accessors.all { it.getBodyExpression() == null }) return null // makes no sense to access backing field - it's the same as accessing property directly

    val bindingContext = resolutionFacade.analyze(declaration)
    if (!bindingContext[BindingContext.BACKING_FIELD_REQUIRED, property]!!) return null

    val lookupElement = createLookupElement(property, true)
    return object : LookupElementDecorator<LookupElement>(lookupElement) {
        override fun getLookupString() = "$" + super.getLookupString()
        override fun getAllLookupStrings() = setOf(getLookupString())

        override fun renderElement(presentation: LookupElementPresentation) {
            super.renderElement(presentation)
            presentation.setItemText("$" + presentation.getItemText())
            presentation.setIcon(PlatformIcons.FIELD_ICON) //TODO: special icon
        }
    }.assignPriority(ItemPriority.BACKING_FIELD)
}

fun LookupElementFactory.createLookupElementForType(type: JetType): LookupElement? {
    if (type.isError()) return null

    if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(type)) {
        val text = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
        val baseLookupElement = LookupElementBuilder.create(text).setIcon(JetIcons.LAMBDA)
        return BaseTypeLookupElement(type, baseLookupElement)
    }
    else {
        val classifier = type.getConstructor().getDeclarationDescriptor() ?: return null
        val baseLookupElement = createLookupElement(classifier, false, qualifyNestedClasses = true, includeClassTypeArguments = false)

        val itemText = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)

        return object : BaseTypeLookupElement(type, baseLookupElement) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setItemText(itemText)
            }
        }
    }
}

private open class BaseTypeLookupElement(type: JetType, baseLookupElement: LookupElement) : LookupElementDecorator<LookupElement>(baseLookupElement) {
    private val fullText = IdeDescriptorRenderers.SOURCE_CODE.renderType(type)

    override fun equals(other: Any?) = other is BaseTypeLookupElement && fullText == other.fullText
    override fun hashCode() = fullText.hashCode()

    override fun renderElement(presentation: LookupElementPresentation) {
        getDelegate().renderElement(presentation)
    }

    override fun handleInsert(context: InsertionContext) {
        context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), fullText)
        context.setTailOffset(context.getStartOffset() + fullText.length())
        shortenReferences(context, context.getStartOffset(), context.getTailOffset())
    }
}

fun shortenReferences(context: InsertionContext, startOffset: Int, endOffset: Int) {
    PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
    ShortenReferences.DEFAULT.process(context.getFile() as JetFile, startOffset, endOffset)
}

fun <T> ElementPattern<T>.and(rhs: ElementPattern<T>) = StandardPatterns.and(this, rhs)
fun <T> ElementPattern<T>.andNot(rhs: ElementPattern<T>) = StandardPatterns.and(this, StandardPatterns.not(rhs))
fun <T> ElementPattern<T>.or(rhs: ElementPattern<T>) = StandardPatterns.or(this, rhs)

fun singleCharPattern(char: Char) = StandardPatterns.character().equalTo(char)
