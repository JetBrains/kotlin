/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix.migration

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.annotations.checkAnnotationName
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.js.PredefinedAnnotation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class MigrateExternalExtensionFix(declaration: KtNamedDeclaration)
    : KotlinQuickFixAction<KtNamedDeclaration>(declaration), CleanupFix {

    override fun getText() = "Fix with 'asDynamic'"
    override fun getFamilyName() = getText()

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val declaration = element ?: return
        val name = declaration.nameAsSafeName
        declaration.modifierList?.annotationEntries?.firstOrNull { it.isJsNative() }?.delete()
        declaration.addModifier(KtTokens.INLINE_KEYWORD)
        declaration.removeModifier(KtTokens.EXTERNAL_KEYWORD)
        if (declaration is KtFunction) {
            declaration.addAnnotation(KotlinBuiltIns.FQ_NAMES.suppress.toSafe(), "\"NOTHING_TO_INLINE\"")
            if (!declaration.hasDeclaredReturnType()) {
                SpecifyTypeExplicitlyIntention.addTypeAnnotation(editor, declaration, declaration.builtIns.unitType)
            }
        }

        val ktPsiFactory = KtPsiFactory(project)
        val body = ktPsiFactory.buildExpression {
            appendName(Name.identifier("asDynamic"))
            appendFixedText("().")
            appendName(name)
            if (declaration is KtNamedFunction) {
                appendParameters(declaration)
            }
        }

        if (declaration is KtNamedFunction) {
            (declaration.bodyExpression as? KtBlockExpression)?.delete()
            declaration.bodyExpression?.replace(body) ?: run {
                declaration.add(ktPsiFactory.createEQ())
                declaration.add(body)
            }
        }
        else if (declaration is KtProperty) {
            declaration.setter?.delete()
            declaration.getter?.delete()
            val getter = ktPsiFactory.createPropertyGetter(body)
            declaration.add(getter)

            if (declaration.isVar) {
                val setterBody = ktPsiFactory.buildExpression {
                    appendName(Name.identifier("asDynamic"))
                    appendFixedText("().")
                    appendName(name)
                    appendFixedText(" = ")
                    appendName(Name.identifier("value"))
                }

                val setterStubProperty = ktPsiFactory.createProperty("val x: Unit set(value) { Unit }")
                val block = setterStubProperty.setter!!.bodyExpression as KtBlockExpression
                block.statements.single().replace(setterBody)
                declaration.add(setterStubProperty.setter!!)
            }
        }
    }

    private fun KtAnnotationEntry.isJsNative(): Boolean {
        val bindingContext = analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        val annotationDescriptor = bindingContext[BindingContext.ANNOTATION, this]
        return annotationDescriptor != null && checkAnnotationName(annotationDescriptor, PredefinedAnnotation.NATIVE.fqName)
    }

    private fun BuilderByPattern<KtExpression>.appendParameters(declaration: KtNamedFunction) {
        appendFixedText("(")
        for ((index, param) in declaration.valueParameters.withIndex()) {
            param.nameAsName?.let { paramName ->
                if (index > 0) {
                    appendFixedText(",")
                }
                appendName(paramName)
            }
        }
        appendFixedText(")")
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val e = diagnostic.psiElement
            if ((e is KtNamedFunction && e.receiverTypeReference != null) ||
                (e is KtProperty && e.receiverTypeReference != null)) {
                return MigrateExternalExtensionFix(e as KtNamedDeclaration)
            }
            return null
        }
    }
}
