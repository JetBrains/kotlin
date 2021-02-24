/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.inplace.MyLookupExpression
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.idea.references.resolveToDescriptors

class MapPlatformClassToKotlinFix(
    element: KtReferenceExpression,
    private val platformClass: ClassDescriptor,
    private val possibleClasses: Collection<ClassDescriptor>
) : KotlinQuickFixAction<KtReferenceExpression>(element) {

    override fun getText(): String {
        val platformClassQualifiedName = DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(platformClass.defaultType)
        val singleClass = possibleClasses.singleOrNull()
        return if (singleClass != null)
            KotlinBundle.message(
                "change.all.usages.of.0.in.this.file.to.1",
                platformClassQualifiedName,
                DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(singleClass.defaultType)
            )
        else
            KotlinBundle.message("change.all.usages.of.0.in.this.file.to.a.kotlin.class", platformClassQualifiedName)
    }

    override fun getFamilyName() = KotlinBundle.message("change.to.kotlin.class")

    public override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val bindingContext = file.analyzeWithContent()

        val imports = ArrayList<KtImportDirective>()
        val usages = ArrayList<KtUserType>()

        for (diagnostic in bindingContext.diagnostics) {
            if (diagnostic.factory !== Errors.PLATFORM_CLASS_MAPPED_TO_KOTLIN) continue

            val refExpr = getImportOrUsageFromDiagnostic(diagnostic) ?: continue
            if (resolveToClass(refExpr, bindingContext) != platformClass) continue

            val import = refExpr.getStrictParentOfType<KtImportDirective>()
            if (import != null) {
                imports.add(import)
            } else {
                usages.add(refExpr.getStrictParentOfType<KtUserType>() ?: continue)
            }
        }

        imports.forEach { it.delete() }

        if (usages.isEmpty()) {
            // if we are not going to replace any usages, there's no reason to continue at all
            return
        }

        val replacedElements = replaceUsagesWithFirstClass(project, usages)

        if (possibleClasses.size > 1 && editor != null) {
            val possibleTypes = LinkedHashSet<String>()
            for (klass in possibleClasses) {
                possibleTypes.add(klass.name.asString())
            }
            buildAndShowTemplate(project, editor, file, replacedElements, possibleTypes)
        }
    }

    private fun replaceUsagesWithFirstClass(project: Project, usages: List<KtUserType>): List<PsiElement> {
        val replacementClass = possibleClasses.first()
        val replacementClassName = replacementClass.name.asString()
        val replacedElements = ArrayList<PsiElement>()
        for (usage in usages) {
            val typeArguments = usage.typeArgumentList
            val typeArgumentsString = typeArguments?.text ?: ""
            val replacementType = KtPsiFactory(project).createType(replacementClassName + typeArgumentsString)
            val replacementTypeElement = replacementType.typeElement!!
            val replacedElement = usage.replace(replacementTypeElement)
            val replacedExpression = replacedElement.firstChild
            assert(replacedExpression is KtSimpleNameExpression) // assumption: the Kotlin class requires no imports
            replacedElements.add(replacedExpression)
        }
        return replacedElements
    }

    private val PRIMARY_USAGE = "PrimaryUsage"
    private val OTHER_USAGE = "OtherUsage"

    private fun buildAndShowTemplate(
        project: Project, editor: Editor, file: PsiFile,
        replacedElements: Collection<PsiElement>, options: LinkedHashSet<String>
    ) {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

        val primaryReplacedExpression = replacedElements.iterator().next()

        val caretModel = editor.caretModel
        val oldOffset = caretModel.offset
        caretModel.moveToOffset(file.node.startOffset)

        val builder = TemplateBuilderImpl(file)
        val expression = MyLookupExpression(
            primaryReplacedExpression.text,
            options,
            null,
            null,
            false,
            KotlinBundle.message("choose.an.appropriate.kotlin.class")
        )

        builder.replaceElement(primaryReplacedExpression, PRIMARY_USAGE, expression, true)
        for (replacedExpression in replacedElements) {
            if (replacedExpression === primaryReplacedExpression) continue
            builder.replaceElement(replacedExpression, OTHER_USAGE, PRIMARY_USAGE, false)
        }

        TemplateManager.getInstance(project).startTemplate(editor, builder.buildInlineTemplate(), object : TemplateEditingAdapter() {
            override fun templateFinished(template: Template, brokenOff: Boolean) {
                caretModel.moveToOffset(oldOffset)
            }
        })
    }

    companion object : KotlinSingleIntentionActionFactoryWithDelegate<KtReferenceExpression, Companion.Data>() {
        data class Data(
            val platformClass: ClassDescriptor,
            val possibleClasses: Collection<ClassDescriptor>
        )

        override fun getElementOfInterest(diagnostic: Diagnostic): KtReferenceExpression? = getImportOrUsageFromDiagnostic(diagnostic)

        override fun extractFixData(element: KtReferenceExpression, diagnostic: Diagnostic): Data? {
            val context = element.analyze(BodyResolveMode.PARTIAL)
            val platformClass = resolveToClass(element, context) ?: return null
            val possibleClasses = Errors.PLATFORM_CLASS_MAPPED_TO_KOTLIN.cast(diagnostic).a
            return Data(platformClass, possibleClasses)
        }

        override fun createFix(originalElement: KtReferenceExpression, data: Data): IntentionAction? {
            return MapPlatformClassToKotlinFix(originalElement, data.platformClass, data.possibleClasses)
        }

        private fun resolveToClass(referenceExpression: KtReferenceExpression, context: BindingContext): ClassDescriptor? {
            return referenceExpression.mainReference.resolveToDescriptors(context).firstIsInstanceOrNull<ClassDescriptor>()
        }

        private fun getImportOrUsageFromDiagnostic(diagnostic: Diagnostic): KtReferenceExpression? {
            val import = diagnostic.psiElement.getNonStrictParentOfType<KtImportDirective>()
            return if (import != null) {
                import.importedReference?.getQualifiedElementSelector() as? KtReferenceExpression
            } else {
                (diagnostic.psiElement.getNonStrictParentOfType<KtUserType>() ?: return null).referenceExpression
            }
        }
    }
}
