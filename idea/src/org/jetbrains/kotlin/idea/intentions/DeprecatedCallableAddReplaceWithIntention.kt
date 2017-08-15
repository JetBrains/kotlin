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
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.core.unblockDocument
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.types.typeUtil.isUnit
import java.util.*

class DeprecatedCallableAddReplaceWithInspection : IntentionBasedInspection<KtCallableDeclaration>(DeprecatedCallableAddReplaceWithIntention::class)

class DeprecatedCallableAddReplaceWithIntention : SelfTargetingRangeIntention<KtCallableDeclaration>(
        KtCallableDeclaration::class.java, "Add 'replaceWith' argument to specify replacement pattern", "Add 'replaceWith' argument to 'Deprecated' annotation"
) {
    private class ReplaceWith(val expression: String, vararg val imports: String)

    override fun applicabilityRange(element: KtCallableDeclaration): TextRange? {
        val annotationEntry = element.deprecatedAnnotationWithNoReplaceWith() ?: return null
        if (element.suggestReplaceWith() == null) return null
        return annotationEntry.textRange
    }

    override fun applyTo(element: KtCallableDeclaration, editor: Editor?) {
        val replaceWith = element.suggestReplaceWith()!!

        assert('\n' !in replaceWith.expression && '\r' !in replaceWith.expression) { "Formatted expression text should not contain \\n or \\r" }

        val annotationEntry = element.deprecatedAnnotationWithNoReplaceWith()!!
        val psiFactory = KtPsiFactory(element)

        var escapedText = replaceWith.expression
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")

        // escape '$' if it's followed by a letter or '{'
        if (escapedText.contains('$')) {
            escapedText = StringBuilder().apply {
                var i = 0
                val length = escapedText.length
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

        val argumentText = StringBuilder().apply {
            append("kotlin.ReplaceWith(\"")
            append(escapedText)
            append("\"")
            //TODO: who should escape keywords here?
            replaceWith.imports.forEach { append(",\"").append(it).append("\"") }
            append(")")
        }.toString()

        var argument = psiFactory.createArgument(psiFactory.createExpression(argumentText))
        argument = annotationEntry.valueArgumentList!!.addArgument(argument)
        argument = ShortenReferences.DEFAULT.process(argument) as KtValueArgument

        editor?.apply {
            unblockDocument()
            moveCaret(argument.textOffset)
        }
    }

    private fun KtCallableDeclaration.deprecatedAnnotationWithNoReplaceWith(): KtAnnotationEntry? {
        val bindingContext = this.analyze()
//        val deprecatedConstructor = KotlinBuiltIns.getInstance().getDeprecatedAnnotation().getUnsubstitutedPrimaryConstructor()
        for (entry in annotationEntries) {
            entry.analyze()
            val resolvedCall = entry.calleeExpression.getResolvedCall(bindingContext) ?: continue
            if (!resolvedCall.isReallySuccess()) continue
//            if (resolvedCall.getResultingDescriptor() != deprecatedConstructor) continue

            //TODO
            val descriptor = resolvedCall.resultingDescriptor.containingDeclaration
            val descriptorFqName = DescriptorUtils.getFqName(descriptor).toSafe()
            if (descriptorFqName != KotlinBuiltIns.FQ_NAMES.deprecated) continue

            val args = resolvedCall.valueArguments.mapKeys { it.key.name.asString() }
            val replaceWithArguments = args["replaceWith"] /*TODO: kotlin.deprecated::replaceWith.name*/
            val level = args["level"]

            if (replaceWithArguments?.arguments?.isNotEmpty() ?: false) return null

            if (level != null && level.arguments.isNotEmpty()) {
                val levelDescriptor = level.arguments[0].getArgumentExpression().getResolvedCall(bindingContext)?.candidateDescriptor
                if (levelDescriptor?.name?.asString() == "HIDDEN") return null
            }

            return entry
        }
        return null
    }

    private fun KtCallableDeclaration.suggestReplaceWith(): ReplaceWith? {
        val replacementExpression = when (this) {
            is KtNamedFunction -> replacementExpressionFromBody()

            is KtProperty -> {
                if (isVar) return null //TODO
                getter?.replacementExpressionFromBody()
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
                if (expression.statements.size > 1) {
                    isGood = false
                    return
                }
                super.visitBlockExpression(expression)
            }

            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                val target = expression.analyze()[BindingContext.REFERENCE_TARGET, expression] as? DeclarationDescriptorWithVisibility ?: return
                if (Visibilities.isPrivate((target.visibility))) {
                    isGood = false
                }
            }

            override fun visitKtElement(element: KtElement) {
                element.acceptChildren(this)
            }
        })
        if (!isGood) return null

        //TODO: check that no receivers that cannot be referenced from outside involved

        val text = replacementExpression.text
        var expression = try {
            KtPsiFactory(this).createExpression(text.replace('\n', ' '))
        }
        catch(e: Throwable) { // does not parse in one line
            return null
        }
        expression = CodeStyleManager.getInstance(project).reformat(expression, true) as KtExpression

        return ReplaceWith(expression.text, *extractImports(replacementExpression).toTypedArray())
    }

    private fun KtDeclarationWithBody.replacementExpressionFromBody(): KtExpression? {
        val body = bodyExpression ?: return null
        if (!hasBlockBody()) return body
        val block = body as? KtBlockExpression ?: return null
        val statement = block.statements.singleOrNull() ?: return null
        val returnsUnit = (analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? FunctionDescriptor)?.returnType?.isUnit() ?: return null
        return when (statement) {
            is KtReturnExpression -> statement.returnedExpression
            else -> if (returnsUnit) statement else null
        }
    }

    private fun extractImports(expression: KtExpression): Collection<String> {
        val file = expression.containingKtFile
        val currentPackageFqName = file.packageFqName
        val importHelper = ImportInsertHelper.getInstance(expression.project)

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
                        && (target.containingDeclaration as? PackageFragmentDescriptor)?.fqName != currentPackageFqName) {
                        result.add(fqName.asString())
                    }
                }
            }

            override fun visitKtElement(element: KtElement) {
                element.acceptChildren(this)
            }
        })
        return result
    }
}