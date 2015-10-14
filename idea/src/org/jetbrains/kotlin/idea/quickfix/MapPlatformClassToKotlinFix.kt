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
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.inplace.MyLookupExpression
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.*

class MapPlatformClassToKotlinFix(
        element: JetReferenceExpression,
        private val platformClass: ClassDescriptor,
        private val possibleClasses: Collection<ClassDescriptor>
) : KotlinQuickFixAction<JetReferenceExpression>(element) {

    override fun getText(): String {
        val platformClassQualifiedName = DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(platformClass.defaultType)
        val singleClass = possibleClasses.singleOrNull()
        return if (singleClass != null)
            "Change all usages of '$platformClassQualifiedName' in this file to '${DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(singleClass.defaultType)}'"
        else
            "Change all usages of '$platformClassQualifiedName' in this file to a Kotlin class"
    }

    override fun getFamilyName() = "Change to Kotlin class"

    public override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        val context = file.analyzeFully()
        val diagnostics = context.diagnostics
        val imports = ArrayList<JetImportDirective>()
        val usages = ArrayList<JetUserType>()

        for (diagnostic in diagnostics) {
            if (diagnostic.factory !== Errors.PLATFORM_CLASS_MAPPED_TO_KOTLIN) continue
            val refExpr = getImportOrUsageFromDiagnostic(diagnostic) ?: continue
            val descriptor = resolveToClass(refExpr, context)
            if (descriptor == null || descriptor != platformClass) continue
            val imp = PsiTreeUtil.getParentOfType(refExpr, JetImportDirective::class.java)
            if (imp == null) {
                val type = PsiTreeUtil.getParentOfType(refExpr, JetUserType::class.java) ?: continue
                usages.add(type)
            } else {
                imports.add(imp)
            }
        }

        for (imp in imports) {
            imp.delete()
        }

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

    private fun replaceUsagesWithFirstClass(project: Project, usages: List<JetUserType>): List<PsiElement> {
        val replacementClass = possibleClasses.iterator().next()
        val replacementClassName = replacementClass.name.asString()
        val replacedElements = ArrayList<PsiElement>()
        for (usage in usages) {
            val typeArguments = usage.typeArgumentList
            val typeArgumentsString = if (typeArguments == null) "" else typeArguments.text
            val replacementType = JetPsiFactory(project).createType(replacementClassName + typeArgumentsString)
            val replacementTypeElement = replacementType.typeElement!!
            val replacedElement = usage.replace(replacementTypeElement)
            val replacedExpression = replacedElement.firstChild
            assert(replacedExpression is JetSimpleNameExpression) // assumption: the Kotlin class requires no imports
            replacedElements.add(replacedExpression)
        }
        return replacedElements
    }

    companion object {
        private val PRIMARY_USAGE = "PrimaryUsage"
        private val OTHER_USAGE = "OtherUsage"

        private fun buildAndShowTemplate(
                project: Project, editor: Editor, file: PsiFile,
                replacedElements: Collection<PsiElement>, options: LinkedHashSet<String>) {
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

            val primaryReplacedExpression = replacedElements.iterator().next()

            val caretModel = editor.caretModel
            val oldOffset = caretModel.offset
            caretModel.moveToOffset(file.node.startOffset)

            val builder = TemplateBuilderImpl(file)
            val expression = MyLookupExpression(primaryReplacedExpression.text, options, null, null, false, "Choose an appropriate Kotlin class")

            builder.replaceElement(primaryReplacedExpression, PRIMARY_USAGE, expression, true)
            for (replacedExpression in replacedElements) {
                if (replacedExpression === primaryReplacedExpression) continue
                builder.replaceElement(replacedExpression, OTHER_USAGE, PRIMARY_USAGE, false)
            }
            TemplateManager.getInstance(project).startTemplate(editor, builder.buildInlineTemplate(), object : TemplateEditingAdapter() {
                override fun templateFinished(template: Template?, brokenOff: Boolean) {
                    caretModel.moveToOffset(oldOffset)
                }
            })
        }

        private fun getImportOrUsageFromDiagnostic(diagnostic: Diagnostic): JetReferenceExpression? {
            val imp = QuickFixUtil.getParentElementOfType(diagnostic, JetImportDirective::class.java)
            val typeExpr: JetReferenceExpression?
            if (imp == null) {
                val type = QuickFixUtil.getParentElementOfType(diagnostic, JetUserType::class.java) ?: return null
                typeExpr = type.referenceExpression
            } else {
                val importRef = imp.importedReference
                if (importRef == null || importRef !is JetDotQualifiedExpression) return null
                val refExpr = (importRef as JetDotQualifiedExpression?)?.getSelectorExpression()
                if (refExpr == null || refExpr !is JetReferenceExpression) return null
                typeExpr = refExpr
            }
            return typeExpr
        }

        fun createFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val typeExpr = getImportOrUsageFromDiagnostic(diagnostic) ?: return null

                    val context = typeExpr.analyze()
                    val platformClass = resolveToClass(typeExpr, context) ?: return null

                    val parametrizedDiagnostic = Errors.PLATFORM_CLASS_MAPPED_TO_KOTLIN.cast(diagnostic)

                    return MapPlatformClassToKotlinFix(typeExpr, platformClass, parametrizedDiagnostic.a)
                }
            }
        }

        private fun resolveToClass(referenceExpression: JetReferenceExpression, context: BindingContext): ClassDescriptor? {
            val descriptor = context.get(BindingContext.REFERENCE_TARGET, referenceExpression)
            val ambiguousTargets = context.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, referenceExpression)
            if (descriptor is ClassDescriptor) {
                return descriptor
            } else if (ambiguousTargets != null) {
                for (target in ambiguousTargets) {
                    if (target is ClassDescriptor) {
                        return target
                    }
                }
            }
            return null
        }
    }
}
