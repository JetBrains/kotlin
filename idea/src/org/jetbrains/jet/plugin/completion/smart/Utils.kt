package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import java.util.HashSet
import com.intellij.codeInsight.lookup.LookupElementDecorator
import org.jetbrains.jet.plugin.completion.handlers.WithTailCharInsertHandler
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.jet.plugin.completion.handlers.WithTailStringInsertHandler
import java.util.ArrayList
import org.jetbrains.jet.plugin.completion.*

class ArtificialElementInsertHandler(
        val textBeforeCaret: String, val textAfterCaret: String, val shortenRefs: Boolean) : InsertHandler<LookupElement>{
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val offset = context.getEditor().getCaretModel().getOffset()
        val startOffset = offset - item.getLookupString().length
        context.getDocument().deleteString(startOffset, offset) // delete inserted lookup string
        context.getDocument().insertString(startOffset, textBeforeCaret + textAfterCaret)
        context.getEditor().getCaretModel().moveToOffset(startOffset + textBeforeCaret.length)

        if (shortenRefs) {
            shortenReferences(context, startOffset, startOffset + textBeforeCaret.length + textAfterCaret.length)
        }
    }
}

fun shortenReferences(context: InsertionContext, startOffset: Int, endOffset: Int) {
    PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
    ShortenReferences.process(context.getFile() as JetFile, startOffset, endOffset)
}

fun mergeTails(tails: Collection<Tail?>): Tail? {
    if (tails.size == 1) return tails.single()
    return if (HashSet(tails).size == 1) tails.first() else null
}

fun LookupElement.addTail(tail: Tail?): LookupElement {
    return when (tail) {
        null -> this

        Tail.COMMA -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailCharInsertHandler(',', true /*TODO: use code style option*/).handleInsert(context, getDelegate())
            }
        }

        Tail.PARENTHESIS -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailCharInsertHandler(')', false).handleInsert(context, getDelegate())
            }
        }
    }
}

fun LookupElement.addTail(matchedExpectedInfos: Collection<ExpectedInfo>): LookupElement
    = addTail(mergeTails(matchedExpectedInfos.map { it.tail }))

fun LookupElement.suppressAutoInsertion() = AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(this)

enum class ExpectedInfoClassification {
    MATCHES
    MAKE_NOT_NULLABLE
    NOT_MATCHES
}

fun MutableCollection<LookupElement>.addLookupElements(expectedInfos: Collection<ExpectedInfo>,
                                                       infoClassifier: (ExpectedInfo) -> ExpectedInfoClassification,
                                                       lookupElementFactory: () -> LookupElement?) {
    val matchedInfos = ArrayList<ExpectedInfo>()
    val matchedInfosNotNullable = ArrayList<ExpectedInfo>()
    for (info in expectedInfos) {
        when (infoClassifier(info)) {
            ExpectedInfoClassification.MATCHES -> matchedInfos.add(info)
            ExpectedInfoClassification.MAKE_NOT_NULLABLE -> matchedInfosNotNullable.add(info)
        }
    }

    if (matchedInfos.isNotEmpty()) {
        val lookupElement = lookupElementFactory()
        if (lookupElement != null) {
            add(lookupElement.addTail(matchedInfos))
        }
    }
    else if (matchedInfosNotNullable.isNotEmpty()) {
        addLookupElementsForNullable(lookupElementFactory, matchedInfosNotNullable)
    }
}

fun MutableCollection<LookupElement>.addLookupElementsForNullable(factory: () -> LookupElement?, matchedInfos: Collection<ExpectedInfo>) {
    var lookupElement = factory()
    if (lookupElement != null) {
        lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement!!) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setItemText("!! " + presentation.getItemText())
            }
            override fun handleInsert(context: InsertionContext) {
                WithTailStringInsertHandler("!!").handleInsert(context, getDelegate())
            }
        }
        lookupElement = lookupElement!!.suppressAutoInsertion()
        add(lookupElement!!.addTail(matchedInfos))
    }

    lookupElement = factory()
    if (lookupElement != null) {
        lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement!!) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setItemText("?: " + presentation.getItemText())
            }
            override fun handleInsert(context: InsertionContext) {
                WithTailStringInsertHandler(" ?: ").handleInsert(context, getDelegate()) //TODO: code style
            }
        }
        lookupElement = lookupElement!!.suppressAutoInsertion()
        add(lookupElement!!.addTail(matchedInfos))
    }
}

fun functionType(function: FunctionDescriptor): JetType? {
    return KotlinBuiltIns.getInstance().getKFunctionType(function.getAnnotations(),
                                                         null,
                                                         function.getValueParameters().map { it.getType() },
                                                         function.getReturnType() ?: return null,
                                                         function.getReceiverParameter() != null)
}

fun JetType.isSubtypeOf(expectedType: JetType) = !isError() && JetTypeChecker.INSTANCE.isSubtypeOf(this, expectedType)

fun <T : Any> T?.toList(): List<T> = if (this != null) listOf(this) else listOf()

fun String?.isNullOrEmpty() = this == null || this.isEmpty()
