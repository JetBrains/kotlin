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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInspection.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getFileTopLevelScope
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.quickfix.JetIntentionAction
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.isAnnotatedAsHidden
import org.jetbrains.kotlin.resolve.scopes.FileScope
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public class ConflictingExtensionPropertyInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        val file = session.file as? JetFile ?: return PsiElementVisitor.EMPTY_VISITOR
        val fileScope = file.getResolutionFacade().getFileTopLevelScope(file)

        return object : JetVisitorVoid() {
            override fun visitProperty(property: JetProperty) {
                super.visitProperty(property)

                if (property.receiverTypeReference != null) {
                    val nameElement = property.nameIdentifier ?: return
                    val propertyDescriptor = property.resolveToDescriptor() as? PropertyDescriptor ?: return

                    val conflictingExtension = conflictingSyntheticExtension(propertyDescriptor, fileScope) ?: return

                    // don't report on hidden declarations
                    if (propertyDescriptor.isAnnotatedAsHidden()) return

                    val fixes = createFixes(property, conflictingExtension, isOnTheFly)

                    val problemDescriptor = holder.manager.createProblemDescriptor(
                            nameElement,
                            "This property conflicts with synthetic extension and should be removed or renamed to avoid breaking code by future changes in the compiler",
                            true,
                            fixes,
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                    holder.registerProblem(problemDescriptor)
                }
            }
        }
    }

    private fun conflictingSyntheticExtension(descriptor: PropertyDescriptor, fileScope: FileScope): SyntheticJavaPropertyDescriptor? {
        val extensionReceiverType = descriptor.extensionReceiverParameter?.type ?: return null
        return fileScope
                .getSyntheticExtensionProperties(listOf(extensionReceiverType), descriptor.name, NoLookupLocation.FROM_IDE)
                .firstIsInstanceOrNull()
    }

    private fun isSameAsSynthetic(declaration: JetProperty, syntheticProperty: SyntheticJavaPropertyDescriptor): Boolean {
        val getter = declaration.getter ?: return false
        val setter = declaration.setter

        if (!checkGetterBodyIsGetMethodCall(getter, syntheticProperty.getMethod)) return false

        if (setter != null) {
            val setMethod = syntheticProperty.setMethod ?: return false // synthetic property is val but our property is var
            if (!checkSetterBodyIsSetMethodCall(setter, setMethod)) return false
        }

        return true
    }

    private fun checkGetterBodyIsGetMethodCall(getter: JetPropertyAccessor, getMethod: FunctionDescriptor): Boolean {
        if (getter.hasBlockBody()) {
            val statement = (getter.bodyExpression as? JetBlockExpression)?.statements?.singleOrNull() ?: return false
            return (statement as? JetReturnExpression)?.returnedExpression.isGetMethodCall(getMethod)
        }
        else {
            return getter.bodyExpression.isGetMethodCall(getMethod)
        }
    }

    private fun checkSetterBodyIsSetMethodCall(setter: JetPropertyAccessor, setMethod: FunctionDescriptor): Boolean {
        val valueParameterName = setter.valueParameters.singleOrNull()?.nameAsName ?: return false
        if (setter.hasBlockBody()) {
            val statement = (setter.bodyExpression as? JetBlockExpression)?.statements?.singleOrNull() ?: return false
            return statement.isSetMethodCall(setMethod, valueParameterName)
        }
        else {
            return setter.bodyExpression.isSetMethodCall(setMethod, valueParameterName)
        }
    }

    private fun JetExpression?.isGetMethodCall(getMethod: FunctionDescriptor): Boolean {
        when (this) {
            is JetCallExpression -> {
                val resolvedCall = getResolvedCall(analyze())
                return resolvedCall != null && resolvedCall.isReallySuccess() && resolvedCall.resultingDescriptor.original == getMethod.original
            }

            is JetQualifiedExpression -> {
                val receiver = receiverExpression
                return receiver is JetThisExpression && receiver.labelQualifier == null && selectorExpression.isGetMethodCall(getMethod)
            }

            else -> return false
        }
    }

    private fun JetExpression?.isSetMethodCall(setMethod: FunctionDescriptor, valueParameterName: Name): Boolean {
        when (this) {
            is JetCallExpression -> {
                if ((valueArguments.singleOrNull()?.getArgumentExpression() as? JetSimpleNameExpression)?.getReferencedNameAsName() != valueParameterName) return false
                val resolvedCall = getResolvedCall(analyze())
                return resolvedCall != null && resolvedCall.isReallySuccess() && resolvedCall.resultingDescriptor.original == setMethod.original
            }

            is JetQualifiedExpression -> {
                val receiver = receiverExpression
                return receiver is JetThisExpression && receiver.labelQualifier == null && selectorExpression.isSetMethodCall(setMethod, valueParameterName)
            }

            else -> return false
        }
    }

    private fun createFixes(property: JetProperty, conflictingExtension: SyntheticJavaPropertyDescriptor, isOnTheFly: Boolean): Array<IntentionWrapper> {
        val fixes = if (isSameAsSynthetic(property, conflictingExtension)) {
            val fix1 = IntentionWrapper(DeleteRedundantExtensionAction(property), property.containingFile)
            // don't add the second fix when on the fly to allow code cleanup
            val fix2 = if (isOnTheFly)
                object : IntentionWrapper(MarkHiddenAndDeprecatedAction(property), property.containingFile), LowPriorityAction {}
            else
                null
            listOf(fix1, fix2).filterNotNull().toTypedArray()
        }
        else {
            emptyArray()
        }
        return fixes
    }

    private class DeleteRedundantExtensionAction(property: JetProperty) : JetIntentionAction<JetProperty>(property) {
        private val LOG = Logger.getInstance(DeleteRedundantExtensionAction::class.java);

        override fun getFamilyName() = "Delete redundant extension property"
        override fun getText() = familyName

        override fun startInWriteAction() = false

        override fun invoke(project: Project, editor: Editor?, file: JetFile) {
            val declaration = element
            val fqName = declaration.resolveToDescriptor().importableFqName
            if (fqName != null) {
                ProgressManager.getInstance().run(
                        object : Task.Modal(project, "Searching for imports to delete", true) {
                            override fun run(indicator: ProgressIndicator) {
                                val importsToDelete = runReadAction {
                                    val searchScope = JetSourceFilterScope.kotlinSources(GlobalSearchScope.projectScope(project), project)
                                    ReferencesSearch.search(declaration, searchScope)
                                            .filterIsInstance<JetSimpleNameReference>()
                                            .map { ref -> ref.expression.getStrictParentOfType<JetImportDirective>() }
                                            .filterNotNull()
                                            .filter { import -> !import.isAllUnder && import.targetDescriptors().size() == 1 }
                                }
                                UIUtil.invokeLaterIfNeeded {
                                    project.executeWriteCommand(text) {
                                        importsToDelete.forEach { import ->
                                            try {
                                                import.delete()
                                            }
                                            catch(e: Exception) {
                                                LOG.error(e)
                                            }
                                        }
                                        declaration.delete()
                                    }
                                }
                            }
                        })
            }
            else {
                project.executeWriteCommand(text) { declaration.delete() }
            }
        }
    }

    private class MarkHiddenAndDeprecatedAction(property: JetProperty) : JetIntentionAction<JetProperty>(property) {
        override fun getFamilyName() = "Mark as @Deprecated(..., level = DeprecationLevel.HIDDEN)"
        override fun getText() = familyName

        override fun invoke(project: Project, editor: Editor?, file: JetFile) {
            val factory = JetPsiFactory(project)
            val name = element.nameAsName!!.render()
            element.addAnnotationWithLineBreak(factory.createAnnotationEntry("@Deprecated(\"Is replaced with automatic synthetic extension\", ReplaceWith(\"$name\"), level = DeprecationLevel.HIDDEN)"))
        }

        //TODO: move into PSI?
        private fun JetNamedDeclaration.addAnnotationWithLineBreak(annotationEntry: JetAnnotationEntry): JetAnnotationEntry {
            val newLine = JetPsiFactory(this).createNewLine()
            if (modifierList != null) {
                val result = addAnnotationEntry(annotationEntry)
                modifierList!!.addAfter(newLine, result)
                return result
            }
            else {
                val result = addAnnotationEntry(annotationEntry)
                addAfter(newLine, modifierList)
                return result
            }
        }
    }
}