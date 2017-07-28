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
import com.intellij.codeInsight.completion.OffsetKey
import com.intellij.codeInsight.completion.OffsetMap
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.util.Key
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.completion.handlers.CastReceiverInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.core.ImportableFqNameClassifier
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.nullability
import java.util.*

tailrec fun <T : Any> LookupElement.putUserDataDeep(key: Key<T>, value: T?) {
    if (this is LookupElementDecorator<*>) {
        getDelegate().putUserDataDeep(key, value)
    }
    else {
        putUserData(key, value)
    }
}

tailrec fun <T : Any> LookupElement.getUserDataDeep(key: Key<T>): T? {
    return if (this is LookupElementDecorator<*>) {
        getDelegate().getUserDataDeep(key)
    }
    else {
        getUserData(key)
    }
}

enum class ItemPriority {
    SUPER_METHOD_WITH_ARGUMENTS,
    FROM_UNRESOLVED_NAME_SUGGESTION,
    GET_OPERATOR,
    DEFAULT,
    IMPLEMENT,
    OVERRIDE,
    STATIC_MEMBER_FROM_IMPORTS,
    STATIC_MEMBER
}

val ITEM_PRIORITY_KEY = Key<ItemPriority>("ITEM_PRIORITY_KEY")

fun LookupElement.assignPriority(priority: ItemPriority): LookupElement {
    putUserData(ITEM_PRIORITY_KEY, priority)
    return this
}

val STATISTICS_INFO_CONTEXT_KEY = Key<String>("STATISTICS_INFO_CONTEXT_KEY")

val NOT_IMPORTED_KEY = Key<Unit>("NOT_IMPORTED_KEY")

fun LookupElement.suppressAutoInsertion() = AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(this)

fun LookupElement.withReceiverCast(): LookupElement {
    return object: LookupElementDecorator<LookupElement>(this) {
        override fun handleInsert(context: InsertionContext) {
            super.handleInsert(context)
            CastReceiverInsertHandler.postHandleInsert(context, delegate)
        }
    }
}

val KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY = Key<Unit>("KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY")

fun LookupElement.keepOldArgumentListOnTab(): LookupElement {
    putUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY, Unit)
    return this
}

fun PrefixMatcher.asNameFilter(): (Name) -> Boolean {
    return { name -> !name.isSpecial && prefixMatches(name.identifier) }
}

fun PrefixMatcher.asStringNameFilter() = { name: String -> prefixMatches(name) }

fun ((String) -> Boolean).toNameFilter(): (Name) -> Boolean {
    return { name -> !name.isSpecial && this(name.identifier) }
}

infix fun <T> ((T) -> Boolean).or(otherFilter: (T) -> Boolean): (T) -> Boolean
        = { this(it) || otherFilter(it) }

fun LookupElementPresentation.prependTailText(text: String, grayed: Boolean) {
    val tails = tailFragments
    clearTail()
    appendTailText(text, grayed)
    tails.forEach { appendTailText(it.text, it.isGrayed) }
}

enum class CallableWeightEnum {
    local, // local non-extension
    thisClassMember,
    baseClassMember,
    thisTypeExtension,
    baseTypeExtension,
    globalOrStatic, // global non-extension
    typeParameterExtension,
    receiverCastRequired
}

class CallableWeight(val enum: CallableWeightEnum, val receiverIndex: Int?) {
    companion object {
        val local = CallableWeight(CallableWeightEnum.local, null)
        val globalOrStatic = CallableWeight(CallableWeightEnum.globalOrStatic, null)
        val receiverCastRequired = CallableWeight(CallableWeightEnum.receiverCastRequired, null)
    }
}

val CALLABLE_WEIGHT_KEY = Key<CallableWeight>("CALLABLE_WEIGHT_KEY")

fun InsertionContext.isAfterDot(): Boolean {
    var offset = startOffset
    val chars = document.charsSequence
    while (offset > 0) {
        offset--
        val c = chars[offset]
        if (!Character.isWhitespace(c)) {
            return c == '.'
        }
    }
    return false
}

// do not complete this items by prefix like "is"
fun shouldCompleteThisItems(prefixMatcher: PrefixMatcher): Boolean {
    val prefix = prefixMatcher.prefix
    val s = "this@"
    return prefix.startsWith(s) || s.startsWith(prefix)
}

class ThisItemLookupObject(val receiverParameter: ReceiverParameterDescriptor, val labelName: Name?) : KeywordLookupObject()

fun ThisItemLookupObject.createLookupElement() = createKeywordElement("this", labelName.labelNameToTail(), lookupObject = this)
        .withTypeText(BasicLookupElementFactory.SHORT_NAMES_RENDERER.renderType(receiverParameter.type))

fun thisExpressionItems(bindingContext: BindingContext, position: KtExpression, prefix: String, resolutionFacade: ResolutionFacade): Collection<ThisItemLookupObject> {
    val scope = position.getResolutionScope(bindingContext, resolutionFacade)

    val psiFactory = KtPsiFactory(position)

    val result = ArrayList<ThisItemLookupObject>()
    for ((receiver, expressionFactory) in scope.getImplicitReceiversWithInstanceToExpression()) {
        if (expressionFactory == null) continue
        // if prefix does not start with "this@" do not include immediate this in the form with label
        val expression = expressionFactory.createExpression(psiFactory, shortThis = !prefix.startsWith("this@")) as? KtThisExpression ?: continue
        result.add(ThisItemLookupObject(receiver, expression.getLabelNameAsName()))
    }
    return result
}

fun returnExpressionItems(bindingContext: BindingContext, position: KtElement): Collection<LookupElement> {
    val result = ArrayList<LookupElement>()
    for (parent in position.parentsWithSelf) {
        if (parent is KtDeclarationWithBody) {
            val returnType = parent.returnType(bindingContext)
            val isUnit = returnType == null || KotlinBuiltIns.isUnit(returnType)
            if (parent is KtFunctionLiteral) {
                val (label, call) = parent.findLabelAndCall()
                if (label != null) {
                    result.add(createKeywordElementWithSpace("return", tail = label.labelNameToTail(), addSpaceAfter = !isUnit))
                }

                // check if the current function literal is inlined and stop processing outer declarations if it's not
                val callee = call?.calleeExpression as? KtReferenceExpression ?: break // not inlined
                if (!InlineUtil.isInline(bindingContext[BindingContext.REFERENCE_TARGET, callee])) break // not inlined
            }
            else {
                if (parent.hasBlockBody()) {
                    result.add(createKeywordElementWithSpace("return", addSpaceAfter = !isUnit))

                    if (returnType != null) {
                        if (returnType.nullability() == TypeNullability.NULLABLE) {
                            result.add(createKeywordElement("return null"))
                        }

                        if (KotlinBuiltIns.isBooleanOrNullableBoolean(returnType)) {
                            result.add(createKeywordElement("return true"))
                            result.add(createKeywordElement("return false"))
                        }
                        else if (KotlinBuiltIns.isCollectionOrNullableCollection(returnType) || KotlinBuiltIns.isListOrNullableList(returnType) || KotlinBuiltIns.isIterableOrNullableIterable(returnType)) {
                            result.add(createKeywordElement("return", tail = " emptyList()"))
                        }
                        else if (KotlinBuiltIns.isSetOrNullableSet(returnType)) {
                            result.add(createKeywordElement("return", tail = " emptySet()"))
                        }
                    }
                }
                break
            }
        }
    }
    return result
}

private fun KtDeclarationWithBody.returnType(bindingContext: BindingContext): KotlinType? {
    val callable = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? CallableDescriptor ?: return null
    return callable.returnType
}

private fun Name?.labelNameToTail(): String = if (this != null) "@" + render() else ""

private fun createKeywordElementWithSpace(
        keyword: String,
        tail: String = "",
        addSpaceAfter: Boolean = false,
        lookupObject: KeywordLookupObject = KeywordLookupObject()
): LookupElement {
    val element = createKeywordElement(keyword, tail, lookupObject)
    return if (addSpaceAfter) {
        object: LookupElementDecorator<LookupElement>(element) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.SPACE.handleInsert(context, delegate)
            }
        }
    }
    else {
        element
    }
}

private fun createKeywordElement(
        keyword: String,
        tail: String = "",
        lookupObject: KeywordLookupObject = KeywordLookupObject()
): LookupElementBuilder {
    var element = LookupElementBuilder.create(lookupObject, keyword + tail)
    element = element.withPresentableText(keyword)
    element = element.withBoldness(true)
    if (tail.isNotEmpty()) {
        element = element.withTailText(tail, false)
    }
    return element
}

fun breakOrContinueExpressionItems(position: KtElement, breakOrContinue: String): Collection<LookupElement> {
    val result = ArrayList<LookupElement>()

    parentsLoop@
    for (parent in position.parentsWithSelf) {
        when (parent) {
            is KtLoopExpression -> {
                if (result.isEmpty()) {
                    result.add(createKeywordElement(breakOrContinue))
                }

                val label = (parent.parent as? KtLabeledExpression)?.getLabelNameAsName()
                if (label != null) {
                    result.add(createKeywordElement(breakOrContinue, tail = label.labelNameToTail()))
                }
            }

            is KtDeclarationWithBody -> break@parentsLoop //TODO: support non-local break's&continue's when they are supported by compiler
        }
    }
    return result
}

fun BasicLookupElementFactory.createLookupElementForType(type: KotlinType): LookupElement? {
    if (type.isError) return null

    return if (type.isFunctionType) {
        val text = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
        val baseLookupElement = LookupElementBuilder.create(text).withIcon(KotlinIcons.LAMBDA)
        BaseTypeLookupElement(type, baseLookupElement)
    }
    else {
        val classifier = type.constructor.declarationDescriptor ?: return null
        val baseLookupElement = createLookupElement(classifier, qualifyNestedClasses = true, includeClassTypeArguments = false)

        val itemText = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)

        val typeLookupElement = object : BaseTypeLookupElement(type, baseLookupElement) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.itemText = itemText
            }
        }

        // if type is simply classifier without anything else, use classifier's lookup element to avoid duplicates (works after "as" in basic completion)
        if (typeLookupElement.fullText == IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(classifier))
            baseLookupElement
        else
            typeLookupElement
    }
}

private open class BaseTypeLookupElement(type: KotlinType, baseLookupElement: LookupElement) : LookupElementDecorator<LookupElement>(baseLookupElement) {
    val fullText = IdeDescriptorRenderers.SOURCE_CODE.renderType(type)

    override fun equals(other: Any?) = other is BaseTypeLookupElement && fullText == other.fullText
    override fun hashCode() = fullText.hashCode()

    override fun renderElement(presentation: LookupElementPresentation) {
        delegate.renderElement(presentation)
    }

    override fun handleInsert(context: InsertionContext) {
        context.document.replaceString(context.startOffset, context.tailOffset, fullText)
        context.tailOffset = context.startOffset + fullText.length
        shortenReferences(context, context.startOffset, context.tailOffset)
    }
}

fun shortenReferences(context: InsertionContext, startOffset: Int, endOffset: Int) {
    PsiDocumentManager.getInstance(context.project).commitAllDocuments()
    ShortenReferences.DEFAULT.process(context.file as KtFile, startOffset, endOffset)
}

infix fun <T> ElementPattern<T>.and(rhs: ElementPattern<T>) = StandardPatterns.and(this, rhs)
fun <T> ElementPattern<T>.andNot(rhs: ElementPattern<T>) = StandardPatterns.and(this, StandardPatterns.not(rhs))
infix fun <T> ElementPattern<T>.or(rhs: ElementPattern<T>) = StandardPatterns.or(this, rhs)

fun singleCharPattern(char: Char) = StandardPatterns.character().equalTo(char)

fun LookupElement.decorateAsStaticMember(
        memberDescriptor: DeclarationDescriptor,
        classNameAsLookupString: Boolean
): LookupElement? {
    val container = memberDescriptor.containingDeclaration as? ClassDescriptor ?: return null
    val classDescriptor = if (container.isCompanionObject)
        container.containingDeclaration as? ClassDescriptor ?: return null
    else
        container

    val containerFqName = container.importableFqName ?: return null
    val qualifierPresentation = classDescriptor.name.asString()

    return object: LookupElementDecorator<LookupElement>(this) {
        override fun getAllLookupStrings(): Set<String> {
            return if (classNameAsLookupString) setOf(delegate.lookupString, qualifierPresentation) else super.getAllLookupStrings()
        }

        override fun renderElement(presentation: LookupElementPresentation) {
            delegate.renderElement(presentation)

            presentation.itemText = qualifierPresentation + "." + presentation.itemText

            val tailText = " (" + DescriptorUtils.getFqName(classDescriptor.containingDeclaration) + ")"
            if (memberDescriptor is FunctionDescriptor) {
                presentation.appendTailText(tailText, true)
            }
            else {
                presentation.setTailText(tailText, true)
            }

            if (presentation.typeText.isNullOrEmpty()) {
                presentation.typeText = BasicLookupElementFactory.SHORT_NAMES_RENDERER.renderType(classDescriptor.defaultType)
            }
        }

        override fun handleInsert(context: InsertionContext) {
            val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
            val file = context.file as KtFile

            val addMemberImport = file.importDirectives.any { !it.isAllUnder && it.importPath?.fqName?.parent() == containerFqName }

            if (addMemberImport) {
                psiDocumentManager.commitAllDocuments()
                ImportInsertHelper.getInstance(context.project).importDescriptor(file, memberDescriptor)
                psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)
            }

            super.handleInsert(context)
        }
    }
}

fun ImportableFqNameClassifier.isImportableDescriptorImported(descriptor: DeclarationDescriptor): Boolean {
    val classification = classify(descriptor.importableFqName!!, false)
    return classification != ImportableFqNameClassifier.Classification.notImported
           && classification != ImportableFqNameClassifier.Classification.siblingImported
}

fun OffsetMap.tryGetOffset(key: OffsetKey): Int? {
    try {
        if (!containsOffset(key)) return null
        return getOffset(key).takeIf { it != -1 } // prior to IDEA 2016.3 getOffset() returned -1 if not found, now it throws exception
    }
    catch(e: Exception) {
        return null
    }
}

var KtCodeFragment.extraCompletionFilter: ((LookupElement) -> Boolean)? by CopyableUserDataProperty(Key.create("EXTRA_COMPLETION_FILTER"))

val DeclarationDescriptor.isArtificialImportAliasedDescriptor: Boolean
    get() = original.name != name