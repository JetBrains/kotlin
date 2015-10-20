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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.quickfix.moveCaret
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.types.typeUtil.isUnit
import java.util.*

public class DeprecatedCallableAddReplaceWithInspection : IntentionBasedInspection<KtCallableDeclaration>(DeprecatedCallableAddReplaceWithIntention())

public class DeprecatedCallableAddReplaceWithIntention : JetSelfTargetingRangeIntention<KtCallableDeclaration>(
        javaClass(), "Add 'replaceWith' argument to specify replacement pattern", "Add 'replaceWith' argument to 'Deprecated' annotation"
) {
    private class ReplaceWith(val expression: String, vararg val imports: String)

    override fun applicabilityRange(element: KtCallableDeclaration): TextRange? {
        val annotationEntry = element.deprecatedAnnotationWithNoReplaceWith() ?: return null
        if (element.suggestReplaceWith() == null) return null
        return annotationEntry.getTextRange()
    }

    override fun applyTo(element: KtCallableDeclaration, editor: Editor) {
        val replaceWith = element.suggestReplaceWith()!!

        assert('\n' !in replaceWith.expression && '\r' !in replaceWith.expression) { "Formatted expression text should not contain \\n or \\r" }

        val annotationEntry = element.deprecatedAnnotationWithNoReplaceWith()!!
        val psiFactory = KtPsiFactory(element)

        var escapedText = replaceWith.expression
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")

        // escape '$' if it's followed by a letter or '{'
        if (escapedText.contains('$')) {
            escapedText = StringBuilder {
                var i = 0
                val length = escapedText.length()
                while (i < length) {
                    val c = escapedText[i++]
                    if (c == '$' && i < length) {
                        val c1 = escapedText[i]
                        if (c1.isJavaIdentifierStart() || c1 == '{') {
                            append('\\')
                        }
                    }
                    append(c)
                }
            }.toString()
        }

        val argumentText = StringBuilder {
            append("kotlin.ReplaceWith(\"")
            append(escapedText)
            append("\"")
            //TODO: who should escape keywords here?
            replaceWith.imports.forEach { append(",\"").append(it).append("\"") }
            append(")")
        }.toString()

        var argument = psiFactory.createArgument(psiFactory.createExpression(argumentText))
        argument = annotationEntry.getValueArgumentList()!!.addArgument(argument)
        argument = ShortenReferences.DEFAULT.process(argument) as KtValueArgument

        PsiDocumentManager.getInstance(argument.getProject()).doPostponedOperationsAndUnblockDocument(editor.getDocument())
        editor.moveCaret(argument.getTextOffset())
    }

    private fun KtCallableDeclaration.deprecatedAnnotationWithNoReplaceWith(): KtAnnotationEntry? {
        val bindingContext = this.analyze()
//        val deprecatedConstructor = KotlinBuiltIns.getInstance().getDeprecatedAnnotation().getUnsubstitutedPrimaryConstructor()
        for (entry in getAnnotationEntries()) {
            entry.analyze()
            val resolvedCall = entry.getCalleeExpression().getResolvedCall(bindingContext) ?: continue
            if (!resolvedCall.isReallySuccess()) continue
//            if (resolvedCall.getResultingDescriptor() != deprecatedConstructor) continue

            //TODO
            val descriptor = resolvedCall.getResultingDescriptor().getContainingDeclaration()
            val descriptorFqName = DescriptorUtils.getFqName(descriptor).toSafe()
            if (descriptorFqName != KotlinBuiltIns.FQ_NAMES.deprecated) continue

            val replaceWithArguments = resolvedCall.getValueArguments().entrySet()
                    .single { it.key.getName().asString() == "replaceWith"/*TODO: kotlin.deprecated::replaceWith.name*/ }.value
            return if (replaceWithArguments.getArguments().isEmpty()) entry else null
        }
        return null
    }

    private fun KtCallableDeclaration.suggestReplaceWith(): ReplaceWith? {
        val replacementExpression = when (this) {
            is KtNamedFunction -> replacementExpressionFromBody()

            is KtProperty -> {
                if (isVar()) return null //TODO
                getGetter()?.replacementExpressionFromBody()
            }

            else -> null
        } ?: return null

        var isGood = true
        replacementExpression.accept(object: KtVisitorVoid(){
            override fun visitReturnExpression(expression: KtReturnExpression) {
                isGood = false
            }

            override fun visitDeclaration(dcl: KtDeclaration) {
                isGood = false
            }

            override fun visitBlockExpression(expression: KtBlockExpression) {
                if (expression.getStatements().size() > 1) {
                    isGood = false
                    return
                }
                super.visitBlockExpression(expression)
            }

            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                val target = expression.analyze()[BindingContext.REFERENCE_TARGET, expression] as? DeclarationDescriptorWithVisibility ?: return
                if (Visibilities.isPrivate((target.getVisibility()))) {
                    isGood = false
                }
            }

            override fun visitJetElement(element: KtElement) {
                element.acceptChildren(this)
            }
        })
        if (!isGood) return null

        //TODO: check that no receivers that cannot be referenced from outside involved

        val text = replacementExpression.getText()
        var expression = try {
            KtPsiFactory(this).createExpression(text.replace('\n', ' '))
        }
        catch(e: Throwable) { // does not parse in one line
            return null
        }
        expression = CodeStyleManager.getInstance(getProject()).reformat(expression, true) as KtExpression

        return ReplaceWith(expression.getText(), *extractImports(replacementExpression).toTypedArray())
    }

    private fun KtDeclarationWithBody.replacementExpressionFromBody(): KtExpression? {
        val body = getBodyExpression() ?: return null
        if (!hasBlockBody()) return body
        val block = body as? KtBlockExpression ?: return null
        val statement = block.getStatements().singleOrNull() as? KtExpression ?: return null
        val returnsUnit = (analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? FunctionDescriptor)?.getReturnType()?.isUnit() ?: return null
        return when (statement) {
            is KtReturnExpression -> statement.getReturnedExpression()
            else -> if (returnsUnit) statement else null
        }
    }

    private fun extractImports(expression: KtExpression): Collection<String> {
        val file = expression.getContainingJetFile()
        val currentPackageFqName = file.getPackageFqName()
        val importHelper = ImportInsertHelper.getInstance(expression.getProject())

        val result = ArrayList<String>()
        expression.accept(object : KtVisitorVoid(){
            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                val bindingContext = expression.analyze()
                val target = bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, expression]
                             ?: bindingContext[BindingContext.REFERENCE_TARGET, expression]
                             ?: return
                if (target.isExtension || expression.getReceiverExpression() == null) {
                    val fqName = target.importableFqName ?: return
                    if (!importHelper.isImportedWithDefault(ImportPath(fqName, false), file)
                        && (target.getContainingDeclaration() as? PackageFragmentDescriptor)?.fqName != currentPackageFqName) {
                        result.add(fqName.asString())
                    }
                }
            }

            override fun visitJetElement(element: KtElement) {
                element.acceptChildren(this)
            }
        })
        return result
    }
}