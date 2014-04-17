package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import java.util.HashSet
import com.intellij.codeInsight.lookup.LookupElementDecorator
import org.jetbrains.jet.plugin.completion.handlers.WithTailInsertHandler
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.checker.JetTypeChecker

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
    val file = context.getFile() as JetFile
    val element = PsiTreeUtil.findElementOfClassAtRange(file, startOffset, endOffset, javaClass<JetElement>())
    if (element != null) {
        ShortenReferences.process(element)
    }
}

fun mergeTails(tails: Collection<Tail?>): Tail? {
    if (tails.size == 1) return tails.single()
    return if (HashSet(tails).size == 1) tails.first() else null
}

fun addTailToLookupElement(lookupElement: LookupElement, tail: Tail?): LookupElement {
    return when (tail) {
        null -> lookupElement

        Tail.COMMA -> object: LookupElementDecorator<LookupElement>(lookupElement) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler(',', true /*TODO: use code style option*/).handleInsert(context, lookupElement)
            }
        }

        Tail.PARENTHESIS -> object: LookupElementDecorator<LookupElement>(lookupElement) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler(')', false).handleInsert(context, lookupElement)
            }
        }
    }
}

fun addTailToLookupElement(lookupElement: LookupElement, matchedExpectedInfos: Collection<ExpectedInfo>): LookupElement
    = addTailToLookupElement(lookupElement, mergeTails(matchedExpectedInfos.map { it.tail }))

fun LookupElement.suppressAutoInsertion() = AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(this)

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
