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
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.intentions.declarations.ConvertMemberToExtensionIntention
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.quickfix.CleanupFix
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.js.PredefinedAnnotation
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class MigrateExternalExtensionFix(declaration: KtNamedDeclaration)
    : KotlinQuickFixAction<KtNamedDeclaration>(declaration), CleanupFix {

    override fun getText() = "Fix with 'asDynamic'"
    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val declaration = element ?: return

        when {
            isMemberExtensionDeclaration(declaration) -> fixExtensionMemberDeclaration(declaration, editor)
            isMemberDeclaration(declaration) -> {
                val containingClass = declaration.containingClassOrObject
                if (containingClass != null) {
                    fixNativeClass(containingClass)
                }
            }
            declaration is KtClassOrObject -> fixNativeClass(declaration)
        }
    }

    private fun fixNativeClass(containingClass: KtClassOrObject) {
        val membersToFix = containingClass.declarations.filterIsInstance<KtCallableDeclaration>().filter { isMemberDeclaration(it) && !isMemberExtensionDeclaration(it) }. map {
             it to fetchJsNativeAnnotations(it)
        }.filter {
            it.second.annotations.isNotEmpty()
        }

        membersToFix.asReversed().forEach { (memberDeclaration, annotations) ->
            if (annotations.nativeAnnotation != null && !annotations.isGetter && !annotations.isSetter && !annotations.isInvoke) {
                convertNativeAnnotationToJsName(memberDeclaration, annotations)
                annotations.nativeAnnotation.delete()
            } else {
                val externalDeclaration = ConvertMemberToExtensionIntention.convert(memberDeclaration)
                fixExtensionMemberDeclaration(externalDeclaration, null) // editor is null as we are not going to open any live templates
            }
        }

        // make class external
        val classAnnotations = fetchJsNativeAnnotations(containingClass)
        fixAnnotations(containingClass, classAnnotations, null)
    }

    private data class JsNativeAnnotations(val annotations: List<KtAnnotationEntry>, val nativeAnnotation: KtAnnotationEntry?, val isGetter: Boolean, val isSetter: Boolean, val isInvoke: Boolean)

    private fun fetchJsNativeAnnotations(declaration: KtNamedDeclaration) : JsNativeAnnotations {
        var isGetter = false
        var isSetter = false
        var isInvoke = false
        var nativeAnnotation: KtAnnotationEntry? = null
        val nativeAnnotations = ArrayList<KtAnnotationEntry>()

        declaration.modifierList?.annotationEntries?.forEach {
            when {
                it.isJsAnnotation(PredefinedAnnotation.NATIVE_GETTER) -> {
                    isGetter = true
                    nativeAnnotations.add(it)
                }
                it.isJsAnnotation(PredefinedAnnotation.NATIVE_SETTER) -> {
                    isSetter = true
                    nativeAnnotations.add(it)
                }
                it.isJsAnnotation(PredefinedAnnotation.NATIVE_INVOKE) -> {
                    isInvoke = true
                    nativeAnnotations.add(it)
                }
                it.isJsAnnotation(PredefinedAnnotation.NATIVE) -> {
                    nativeAnnotations.add(it)
                    nativeAnnotation = it
                }
            }
        }
        return JsNativeAnnotations(nativeAnnotations, nativeAnnotation, isGetter, isSetter, isInvoke)
    }

    private fun fixExtensionMemberDeclaration(declaration: KtNamedDeclaration, editor: Editor?) {
        val name = declaration.nameAsSafeName
        val annotations = fetchJsNativeAnnotations(declaration)
        fixAnnotations(declaration, annotations, editor)

        val ktPsiFactory = KtPsiFactory(declaration)
        val body = ktPsiFactory.buildExpression {
            appendName(Name.identifier("asDynamic"))
            when {
                annotations.isGetter -> {
                    appendFixedText("()")
                    if (declaration is KtNamedFunction) {
                        appendParameters(declaration, "[", "]")
                    }
                }
                annotations.isSetter -> {
                    appendFixedText("()")
                    if (declaration is KtNamedFunction) {
                        appendParameters(declaration, "[", "]", skipLast = true)
                        declaration.valueParameters.last().nameAsName?.let {
                            appendFixedText(" = ")
                            appendName(it)
                        }
                    }
                }
                annotations.isInvoke -> {
                    appendFixedText("()")
                    if (declaration is KtNamedFunction) {
                        appendParameters(declaration, "(", ")")
                    }
                }
                else -> {
                    appendFixedText("().")
                    appendName(name)
                    if (declaration is KtNamedFunction) {
                        appendParameters(declaration, "(", ")")
                    }
                }
            }
        }

        if (declaration is KtNamedFunction) {
            declaration.bodyExpression?.delete()
            declaration.equalsToken?.delete()

            if (annotations.isSetter || annotations.isInvoke) {
                val blockBody = ktPsiFactory.createSingleStatementBlock(body)
                declaration.add(blockBody)
            } else {
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

    private fun fixAnnotations(declaration: KtNamedDeclaration, annotations: JsNativeAnnotations, editor: Editor?) {
        annotations.annotations.forEach { it.delete() }

        if (declaration is KtClassOrObject) {
            declaration.addModifier(KtTokens.EXTERNAL_KEYWORD)
        } else {
            declaration.addModifier(KtTokens.INLINE_KEYWORD)
            declaration.removeModifier(KtTokens.EXTERNAL_KEYWORD)
        }

        if (declaration is KtFunction) {
            declaration.addAnnotation(KotlinBuiltIns.FQ_NAMES.suppress, "\"NOTHING_TO_INLINE\"")
        }

        convertNativeAnnotationToJsName(declaration, annotations)

        if (declaration is KtFunction && !declaration.hasDeclaredReturnType() && !annotations.isSetter && !annotations.isInvoke && editor != null) {
            SpecifyTypeExplicitlyIntention.addTypeAnnotation(editor, declaration, declaration.builtIns.unitType)
        }
    }

    private fun convertNativeAnnotationToJsName(declaration: KtNamedDeclaration, annotations: JsNativeAnnotations) {
        val nativeAnnotation = annotations.nativeAnnotation
        if (nativeAnnotation != null && nativeAnnotation.valueArguments.isNotEmpty()) {
            declaration.addAnnotation(FqName("JsName"), nativeAnnotation.valueArguments.joinToString { it.asElement().text })
        }
    }

    private fun BuilderByPattern<KtExpression>.appendParameters(declaration: KtNamedFunction, lParenth: String, rParenth: String, skipLast: Boolean = false) {
        appendFixedText(lParenth)
        for ((index, param) in declaration.valueParameters.let { if (skipLast) it.take(it.size-1) else it }.withIndex()) {
            param.nameAsName?.let { paramName ->
                if (index > 0) {
                    appendFixedText(",")
                }
                appendName(paramName)
            }
        }
        appendFixedText(rParenth)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        private fun KtAnnotationEntry.isJsAnnotation(vararg predefinedAnnotations: PredefinedAnnotation): Boolean {
            val bindingContext = analyze(BodyResolveMode.PARTIAL)
            val annotationDescriptor = bindingContext[BindingContext.ANNOTATION, this]
            return annotationDescriptor != null && predefinedAnnotations.any { annotationDescriptor.fqName == it.fqName }
        }

        private fun KtAnnotationEntry.isJsNativeAnnotation(): Boolean {
            return isJsAnnotation(PredefinedAnnotation.NATIVE, PredefinedAnnotation.NATIVE_GETTER, PredefinedAnnotation.NATIVE_SETTER, PredefinedAnnotation.NATIVE_INVOKE )
        }

        private fun isMemberExtensionDeclaration(psiElement: PsiElement): Boolean {
            return (psiElement is KtNamedFunction && psiElement.receiverTypeReference != null) ||
                   (psiElement is KtProperty && psiElement.receiverTypeReference != null)
        }

        private fun isMemberDeclaration(psiElement: PsiElement): Boolean {
            return (psiElement is KtNamedFunction && psiElement.receiverTypeReference == null) ||
                   (psiElement is KtProperty && psiElement.receiverTypeReference == null)
        }

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val e = diagnostic.psiElement
            when (diagnostic.factory) {
                ErrorsJs.WRONG_EXTERNAL_DECLARATION -> {
                    if (isMemberExtensionDeclaration(e) && e.getParentOfType<KtClassOrObject>(true) == null) {
                        return MigrateExternalExtensionFix(e as KtNamedDeclaration)
                    }
                }
                Errors.DEPRECATION_ERROR, Errors.DEPRECATION -> {
                    if (e.getParentOfType<KtAnnotationEntry>(false)?.isJsNativeAnnotation() == true) {
                        e.getParentOfType<KtNamedDeclaration>(false)?.let {
                            return MigrateExternalExtensionFix(it)
                        }
                    }
                    if ((e as? KtNamedDeclaration)?.modifierList?.annotationEntries?.any { it.isJsNativeAnnotation() } == true) {
                        return MigrateExternalExtensionFix(e as KtNamedDeclaration)
                    }
                }
            }

            return null
        }
    }
}
