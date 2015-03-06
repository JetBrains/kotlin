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
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.completion.handlers.CastReceiverInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.getImplicitReceiversWithInstance
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import java.util.ArrayList

enum class ItemPriority {
    MULTIPLE_ARGUMENTS_ITEM
    DEFAULT
    BACKING_FIELD
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

fun PrefixMatcher.asNameFilter() = { (name: Name) ->
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
    local // local non-extension
    thisClassMember
    baseClassMember
    thisTypeExtension
    baseTypeExtension
    global // global non-extension
    notApplicableReceiverNullable
}

val CALLABLE_WEIGHT_KEY = Key<CallableWeight>("CALLABLE_WEIGHT_KEY")

fun descriptorsEqualWithSubstitution(descriptor1: DeclarationDescriptor?, descriptor2: DeclarationDescriptor?): Boolean {
    if (descriptor1 == descriptor2) return true
    if (descriptor1 == null || descriptor2 == null) return false
    if (descriptor1.getOriginal() != descriptor2.getOriginal()) return false
    if (descriptor1 !is CallableDescriptor) return true
    descriptor2 as CallableDescriptor

    // optimization:
    if (descriptor1 == descriptor1.getOriginal() && descriptor2 == descriptor2.getOriginal()) return true

    if (descriptor1.getReturnType() != descriptor2.getReturnType()) return false
    val parameters1 = descriptor1.getValueParameters()
    val parameters2 = descriptor2.getValueParameters()
    if (parameters1.size() != parameters2.size()) return false
    for (i in parameters1.indices) {
        if (parameters1[i].getType() != parameters2[i].getType()) return false
    }
    return true
}

fun DeclarationDescriptorWithVisibility.isVisible(
        from: DeclarationDescriptor,
        bindingContext: BindingContext? = null,
        element: JetSimpleNameExpression? = null
): Boolean {
    if (Visibilities.isVisible(ReceiverValue.IRRELEVANT_RECEIVER, this, from)) return true
    if (bindingContext == null || element == null) return false

    val receiver = element.getReceiverExpression()
    val type = receiver?.let { bindingContext.get(BindingContext.EXPRESSION_TYPE, it) }
    val explicitReceiver = type?.let { ExpressionReceiver(receiver, it) }

    if (explicitReceiver != null) {
        val normalizeReceiver = ExpressionTypingUtils.normalizeReceiverValueForVisibility(explicitReceiver, bindingContext)
        return Visibilities.isVisible(normalizeReceiver, this, from)
    }

    val jetScope = bindingContext[BindingContext.RESOLUTION_SCOPE, element]
    val implicitReceivers = jetScope?.getImplicitReceiversHierarchy()
    if (implicitReceivers != null) {
        for (implicitReceiver in implicitReceivers) {
            val normalizeReceiver = ExpressionTypingUtils.normalizeReceiverValueForVisibility(implicitReceiver.getValue(), bindingContext)
            if (Visibilities.isVisible(normalizeReceiver, this, from)) return true
        }
    }
    return false
}

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

data class ThisItemInfo(val factory: () -> LookupElement, val type: FuzzyType)

fun thisExpressionItems(bindingContext: BindingContext, position: JetExpression, prefix: String): Collection<ThisItemInfo> {
    val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, position] ?: return listOf()

    val result = ArrayList<ThisItemInfo>()
    for ((i, receiver) in scope.getImplicitReceiversWithInstance().withIndex()) {
        val thisType = receiver.getType()
        val fuzzyType = FuzzyType(thisType, listOf())

        fun createLookupElement(label: String?): LookupElement {
            var element = createKeywordWithLabelElement("this", label)
            element = element.withTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(thisType))
            return element
        }

        if (i == 0) {
            result.add(ThisItemInfo({ createLookupElement(null) }, fuzzyType))
            if (!prefix.startsWith("this@")) continue // if prefix does not start with "this@" do not include immediate this in the form with label
        }

        val label = thisQualifierName(receiver) ?: continue
        result.add(ThisItemInfo({ createLookupElement(label) }, fuzzyType))
    }
    return result
}

private fun thisQualifierName(receiver: ReceiverParameterDescriptor): String? {
    val descriptor = receiver.getContainingDeclaration()
    val name = descriptor.getName()
    if (!name.isSpecial()) {
        return name.asString()
    }

    val functionLiteral = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? JetFunctionLiteral ?: return null
    return functionLiteralLabel(functionLiteral)
}

private fun functionLiteralLabel(functionLiteral: JetFunctionLiteral): String?
        = functionLiteralLabelAndCall(functionLiteral).first

private fun functionLiteralLabelAndCall(functionLiteral: JetFunctionLiteral): Pair<String?, JetCallExpression?> {
    val literalParent = (functionLiteral.getParent() as JetFunctionLiteralExpression).getParent()

    fun JetValueArgument.callExpression(): JetCallExpression? {
        val parent = getParent()
        return (if (parent is JetValueArgumentList) parent else this).getParent() as? JetCallExpression
    }

    when (literalParent) {
        is JetLabeledExpression -> {
            val callExpression = (literalParent.getParent() as? JetValueArgument)?.callExpression()
            return Pair(literalParent.getLabelName(), callExpression)
        }

        is JetValueArgument -> {
            val callExpression = literalParent.callExpression()
            val label = (callExpression?.getCalleeExpression() as? JetSimpleNameExpression)?.getReferencedName()
            return Pair(label, callExpression)
        }

        else -> {
            return Pair(null, null)
        }
    }
}

fun returnExpressionItems(bindingContext: BindingContext, position: JetElement): Collection<LookupElement> {
    val result = ArrayList<LookupElement>()
    for (parent in position.parents()) {
        if (parent is JetDeclarationWithBody) {
            val returnsUnit = returnsUnit(parent, bindingContext)
            if (parent is JetFunctionLiteral) {
                val (label, call) = functionLiteralLabelAndCall(parent)
                if (label != null) {
                    result.add(createKeywordWithLabelElement("return", label, addSpace = !returnsUnit))
                }

                // check if the current function literal is inlined and stop processing outer declarations if it's not
                val callee = call?.getCalleeExpression() as? JetReferenceExpression ?: break // not inlined
                val target = bindingContext[BindingContext.REFERENCE_TARGET, callee] as? SimpleFunctionDescriptor ?: break // not inlined
                if (!target.getInlineStrategy().isInline()) break // not inlined
            }
            else {
                if (parent.hasBlockBody()) {
                    result.add(createKeywordWithLabelElement("return", null, addSpace = !returnsUnit))
                }
                break
            }
        }
    }
    return result
}

private fun returnsUnit(declaration: JetDeclarationWithBody, bindingContext: BindingContext): Boolean {
    val callable = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? CallableDescriptor ?: return true
    val returnType = callable.getReturnType() ?: return true
    return KotlinBuiltIns.isUnit(returnType)
}

private fun createKeywordWithLabelElement(keyword: String, label: String?, addSpace: Boolean): LookupElement {
    val element = createKeywordWithLabelElement(keyword, label)
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

private fun createKeywordWithLabelElement(keyword: String, label: String?): LookupElementBuilder {
    var element = LookupElementBuilder.create(KeywordLookupObject, if (label == null) keyword else "$keyword@$label")
    element = element.withPresentableText(keyword)
    element = element.withBoldness(true)
    if (label != null) {
        element = element.withTailText("@$label", false)
    }
    return element
}

fun breakOrContinueExpressionItems(position: JetElement, breakOrContinue: String): Collection<LookupElement> {
    val result = ArrayList<LookupElement>()
    for (parent in position.parents()) {
        when (parent) {
            is JetLoopExpression -> {
                if (result.isEmpty()) {
                    result.add(createKeywordWithLabelElement(breakOrContinue, null))
                }

                val label = (parent.getParent() as? JetLabeledExpression)?.getLabelName()
                if (label != null) {
                    result.add(createKeywordWithLabelElement(breakOrContinue, label))
                }
            }

            is JetDeclarationWithBody -> break //TODO: support non-local break's&continue's when they are supported by compiler
        }
    }
    return result
}

fun LookupElementFactory.createBackingFieldLookupElement(
        property: PropertyDescriptor,
        inDescriptor: DeclarationDescriptor?,
        resolutionFacade: ResolutionFacade
): LookupElement? {
    if (inDescriptor == null) return null // no backing field accessible
    val insideAccessor = inDescriptor is PropertyAccessorDescriptor && inDescriptor.getCorrespondingProperty() == property
    if (!insideAccessor) {
        val container = property.getContainingDeclaration()
        if (container !is ClassDescriptor || !DescriptorUtils.isAncestor(container, inDescriptor, false)) return null // backing field not accessible
    }

    val declaration = (DescriptorToSourceUtils.descriptorToDeclaration(property) as? JetProperty) ?: return null

    val accessors = declaration.getAccessors()
    if (accessors.all { it.getBodyExpression() == null }) return null // makes no sense to access backing field - it's the same as accessing property directly

    val bindingContext = resolutionFacade.analyze(declaration)
    if (!bindingContext[BindingContext.BACKING_FIELD_REQUIRED, property]) return null

    val lookupElement = createLookupElement(resolutionFacade, property, true)
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
