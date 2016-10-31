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

package org.jetbrains.kotlin.android.intention

import com.android.resources.ResourceType
import com.intellij.CommonBundle
import com.intellij.codeInsight.template.*
import com.intellij.codeInsight.template.impl.*
import com.intellij.codeInsight.template.macro.VariableOfTypeMacro
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.UndoUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.android.actions.CreateXmlResourceDialog
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.util.AndroidBundle
import org.jetbrains.android.util.AndroidResourceUtil
import org.jetbrains.android.util.AndroidUtils
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode


class KotlinAndroidAddStringResource : SelfTargetingIntention<KtLiteralStringTemplateEntry>(KtLiteralStringTemplateEntry::class.java,
                                                                                            "Extract string resource") {
    private companion object {
        private val CLASS_CONTEXT = "android.content.Context"
        private val CLASS_FRAGMENT = "android.app.Fragment"
        private val CLASS_SUPPORT_FRAGMENT = "android.support.v4.app.Fragment"
        private val CLASS_VIEW = "android.view.View"

        private val GET_STRING_METHOD = "getString"
        private val EXTRACT_RESOURCE_DIALOG_TITLE = "Extract Resource"
        private val PACKAGE_NOT_FOUND_ERROR = "package.not.found.error"
        private val RESOURCE_DIR_ERROR = "check.resource.dir.error"
    }

    override fun startInWriteAction(): Boolean = false

    override fun isApplicableTo(element: KtLiteralStringTemplateEntry, caretOffset: Int): Boolean {
        if (AndroidFacet.getInstance(element.containingFile) == null) {
            return false
        }

        // Should not be available to strings with template expressions
        // only to strings with single KtLiteralStringTemplateEntry inside
        return element.parent.children.size == 1
    }

    override fun applyTo(element: KtLiteralStringTemplateEntry, editor: Editor?) {
        val facet = AndroidFacet.getInstance(element.containingFile)
        if (editor == null) {
            throw IllegalArgumentException("This intention requires an editor.")
        }

        if (facet == null) {
            throw IllegalStateException("This intention requires android facet.")
        }

        val file = element.containingFile as KtFile
        val project = file.project

        val manifestPackage = getManifestPackage(facet)
        if (manifestPackage == null) {
            Messages.showErrorDialog(project, AndroidBundle.message(PACKAGE_NOT_FOUND_ERROR), CommonBundle.getErrorTitle())
            return
        }

        val parameters = getCreateXmlResourceParameters(facet.module, element, file.virtualFile) ?: return

        runWriteAction {
            if (!AndroidResourceUtil.createValueResource(project, parameters.resourceDirectory, parameters.name, ResourceType.STRING,
                                                         parameters.fileName, parameters.directoryNames, parameters.value)) {
                return@runWriteAction
            }

            createResourceReference(facet.module, editor, file, element, manifestPackage, parameters.name, ResourceType.STRING)
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            UndoUtil.markPsiFileForUndo(file)
        }
    }

    private fun getCreateXmlResourceParameters(module: Module, element: KtLiteralStringTemplateEntry,
                                               contextFile: VirtualFile): CreateXmlResourceParameters? {

        val stringValue = element.text

        val showDialog = !ApplicationManager.getApplication().isUnitTestMode
        val resourceName = element.getUserData(CREATE_XML_RESOURCE_PARAMETERS_NAME_KEY)

        val dialog = CreateXmlResourceDialog(module, ResourceType.STRING, resourceName, stringValue, true, null, contextFile)
        dialog.title = EXTRACT_RESOURCE_DIALOG_TITLE
        if (showDialog) {
            if (!dialog.showAndGet()) {
                return null
            }
        }
        else {
            dialog.close(0)
        }

        val resourceDirectory = dialog.resourceDirectory
        if (resourceDirectory == null) {
            AndroidUtils.reportError(module.project, AndroidBundle.message(RESOURCE_DIR_ERROR, module))
            return null
        }

        return CreateXmlResourceParameters(dialog.resourceName,
                                           dialog.value,
                                           dialog.fileName,
                                           resourceDirectory,
                                           dialog.dirNames)
    }

    private fun createResourceReference(module: Module, editor: Editor, file: KtFile, element: PsiElement, aPackage: String,
                                        resName: String, resType: ResourceType) {
        val rFieldName = AndroidResourceUtil.getRJavaFieldName(resName)
        val fieldName = "$aPackage.R.$resType.$rFieldName"

        val template: TemplateImpl
        if (!needContextReceiver(element)) {
            template = TemplateImpl("", "$GET_STRING_METHOD($fieldName)", "")
        }
        else {
            template = TemplateImpl("", "\$context\$.$GET_STRING_METHOD($fieldName)", "")
            val marker = MacroCallNode(VariableOfTypeMacro())
            marker.addParameter(ConstantNode(CLASS_CONTEXT))
            template.addVariable("context", marker, ConstantNode("context"), true)
        }

        val containingLiteralExpression = element.parent
        editor.caretModel.moveToOffset(containingLiteralExpression.textOffset)
        editor.document.deleteString(containingLiteralExpression.textRange.startOffset, containingLiteralExpression.textRange.endOffset)
        val marker = editor.document.createRangeMarker(containingLiteralExpression.textOffset, containingLiteralExpression.textOffset)
        marker.isGreedyToLeft = true
        marker.isGreedyToRight = true

        TemplateManager.getInstance(module.project).startTemplate(editor, template, false, null, object : TemplateEditingAdapter() {
            override fun waitingForInput(template: Template?) {
                ShortenReferences.DEFAULT.process(file, marker.startOffset, marker.endOffset)
            }

            override fun beforeTemplateFinished(state: TemplateState?, template: Template?) {
                ShortenReferences.DEFAULT.process(file, marker.startOffset, marker.endOffset)
            }
        })
    }

    private fun needContextReceiver(element: PsiElement): Boolean {
        val classesWithGetSting = listOf(CLASS_CONTEXT, CLASS_FRAGMENT, CLASS_SUPPORT_FRAGMENT)
        val viewClass = listOf(CLASS_VIEW)
        var parent = PsiTreeUtil.findFirstParent(element, true) { it is KtClassOrObject || it is KtFunction || it is KtLambdaExpression }

        while (parent != null) {

            if (parent.isSubclassOrSubclassExtension(classesWithGetSting)) {
                return false
            }

            if (parent.isSubclassOrSubclassExtension(viewClass) ||
                (parent is KtClassOrObject && !parent.isInnerClass() && !parent.isObjectLiteral())) {
                return true
            }

            parent = PsiTreeUtil.findFirstParent(parent, true) { it is KtClassOrObject || it is KtFunction || it is KtLambdaExpression }
        }

        return true
    }

    private fun getManifestPackage(facet: AndroidFacet) = facet.manifest?.`package`?.value

    private fun PsiElement.isSubclassOrSubclassExtension(baseClasses: Collection<String>) =
            (this as? KtClassOrObject)?.isSubclassOfAny(baseClasses) ?:
            this.isSubclassExtensionOfAny(baseClasses)

    private fun PsiElement.isSubclassExtensionOfAny(baseClasses: Collection<String>) =
            (this as? KtLambdaExpression)?.isSubclassExtensionOfAny(baseClasses) ?:
            (this as? KtFunction)?.isSubclassExtensionOfAny(baseClasses) ?:
            false

    private fun KtClassOrObject.isObjectLiteral() = (this as? KtObjectDeclaration)?.isObjectLiteral() ?: false

    private fun KtClassOrObject.isInnerClass() = (this as? KtClass)?.isInner() ?: false

    private fun KtFunction.isSubclassExtensionOfAny(baseClasses: Collection<String>): Boolean {
        val descriptor = resolveToDescriptor() as FunctionDescriptor
        val extendedTypeDescriptor = descriptor.extensionReceiverParameter?.type?.constructor?.declarationDescriptor
        return extendedTypeDescriptor != null && baseClasses.any { extendedTypeDescriptor.isSubclassOf(it) }
    }

    private fun KtLambdaExpression.isSubclassExtensionOfAny(baseClasses: Collection<String>): Boolean {
        val bindingContext = analyze(BodyResolveMode.PARTIAL)
        val type = bindingContext.getType(this)

        if (type == null || !type.isExtensionFunctionType) {
            return false
        }

        val extendedTypeDescriptor = type.arguments.first().type.constructor.declarationDescriptor
        if (extendedTypeDescriptor != null) {
            return baseClasses.any { extendedTypeDescriptor.isSubclassOf(it) }
        }

        return false
    }

    private fun KtClassOrObject.isSubclassOfAny(baseClasses: Collection<String>): Boolean {
        val bindingContext = analyze(BodyResolveMode.PARTIAL)
        val declarationDescriptor = bindingContext.get(BindingContext.CLASS, this)
        return baseClasses.any { declarationDescriptor?.isSubclassOf(it) ?: false }
    }

    private fun ClassifierDescriptor.isSubclassOf(className: String): Boolean {
        return fqNameSafe.asString() == className || isStrictSubclassOf(className)
    }

    private fun ClassifierDescriptor.isStrictSubclassOf(className: String) = defaultType.constructor.supertypes.any {
        it.constructor.declarationDescriptor?.isSubclassOf(className) ?: false
    }
}
