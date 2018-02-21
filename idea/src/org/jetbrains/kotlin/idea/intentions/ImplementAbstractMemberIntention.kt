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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.ide.util.PsiClassListCellRenderer
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.ui.components.JBList
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideImplementMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.idea.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import org.jetbrains.kotlin.util.findCallableMemberBySignature
import java.util.*
import javax.swing.ListSelectionModel

abstract class ImplementAbstractMemberIntentionBase :
        SelfTargetingRangeIntention<KtNamedDeclaration>(KtNamedDeclaration::class.java, "", "Implement abstract member") {
    companion object {
        private val LOG = Logger.getInstance("#${ImplementAbstractMemberIntentionBase::class.java.canonicalName}")
    }

    protected fun findExistingImplementation(
            subClass: ClassDescriptor,
            superMember: CallableMemberDescriptor
    ): CallableMemberDescriptor? {
        val superClass = superMember.containingDeclaration as? ClassDescriptor ?: return null
        val substitutor = getTypeSubstitutor(superClass.defaultType, subClass.defaultType) ?: TypeSubstitutor.EMPTY
        val signatureInSubClass = superMember.substitute(substitutor) as? CallableMemberDescriptor ?: return null
        val subMember = subClass.findCallableMemberBySignature(signatureInSubClass)
        return if (subMember?.kind?.isReal == true) subMember else null
    }

    protected abstract fun acceptSubClass(subClassDescriptor: ClassDescriptor, memberDescriptor: CallableMemberDescriptor): Boolean

    private fun findClassesToProcess(member: KtNamedDeclaration): Sequence<PsiElement> {
        val baseClass = member.containingClassOrObject as? KtClass ?: return emptySequence()
        val memberDescriptor = member.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: return emptySequence()

        fun acceptSubClass(subClass: PsiElement): Boolean {
            val classDescriptor = when (subClass) {
                is KtLightClass -> subClass.kotlinOrigin?.resolveToDescriptorIfAny()
                is KtEnumEntry -> subClass.resolveToDescriptorIfAny()
                is PsiClass -> subClass.getJavaClassDescriptor()
                else -> null
            } ?: return false
            return acceptSubClass(classDescriptor, memberDescriptor)
        }

        if (baseClass.isEnum()) {
            return baseClass.declarations
                    .asSequence()
                    .filterIsInstance<KtEnumEntry>()
                    .filter(::acceptSubClass)
        }

        return HierarchySearchRequest(baseClass, baseClass.useScope, false)
                .searchInheritors()
                .asSequence()
                .filter(::acceptSubClass)
    }

    protected abstract fun computeText(element: KtNamedDeclaration): String?

    override fun applicabilityRange(element: KtNamedDeclaration): TextRange? {
        if (!element.isAbstract()) return null

        text = computeText(element) ?: return null

        if (!findClassesToProcess(element).any()) return null

        return element.nameIdentifier?.textRange
    }

    protected abstract val preferConstructorParameters: Boolean

    private fun implementInKotlinClass(editor: Editor?, member: KtNamedDeclaration, targetClass: KtClassOrObject) {
        val subClassDescriptor = targetClass.resolveToDescriptorIfAny() ?: return
        val superMemberDescriptor = member.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: return
        val superClassDescriptor = superMemberDescriptor.containingDeclaration as? ClassDescriptor ?: return
        val substitutor = getTypeSubstitutor(superClassDescriptor.defaultType, subClassDescriptor.defaultType)
                          ?: TypeSubstitutor.EMPTY
        val descriptorToImplement = superMemberDescriptor.substitute(substitutor) as CallableMemberDescriptor
        val chooserObject = OverrideMemberChooserObject.create(member.project,
                                                               descriptorToImplement,
                                                               descriptorToImplement,
                                                               OverrideMemberChooserObject.BodyType.EMPTY,
                                                               preferConstructorParameters)
        OverrideImplementMembersHandler.generateMembers(editor, targetClass, listOf(chooserObject), false)
    }

    private fun implementInJavaClass(member: KtNamedDeclaration, targetClass: PsiClass) {
        member.toLightMethods().forEach { OverrideImplementUtil.overrideOrImplement(targetClass, it) }
    }

    private fun implementInClass(member: KtNamedDeclaration, targetClasses: List<PsiElement>) {
        val project = member.project
        project.executeCommand(CodeInsightBundle.message("intention.implement.abstract.method.command.name")) {
            if (!FileModificationService.getInstance().preparePsiElementsForWrite(targetClasses)) return@executeCommand
            runWriteAction {
                for (targetClass in targetClasses) {
                    try {
                        val descriptor = OpenFileDescriptor(project, targetClass.containingFile.virtualFile)
                        val targetEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)!!
                        when (targetClass) {
                            is KtLightClass -> targetClass.kotlinOrigin?.let { implementInKotlinClass(targetEditor, member, it) }
                            is KtEnumEntry -> implementInKotlinClass(targetEditor, member, targetClass)
                            is PsiClass -> implementInJavaClass(member, targetClass)
                        }
                    }
                    catch(e: IncorrectOperationException) {
                        LOG.error(e)
                    }
                }
            }
        }
    }

    private class ClassRenderer : PsiElementListCellRenderer<PsiElement>() {
        private val psiClassRenderer = PsiClassListCellRenderer()

        override fun getComparator(): Comparator<PsiElement> {
            val baseComparator = psiClassRenderer.comparator
            return Comparator { o1, o2 ->
                when {
                    o1 is KtEnumEntry && o2 is KtEnumEntry -> o1.name!!.compareTo(o2.name!!)
                    o1 is KtEnumEntry -> -1
                    o2 is KtEnumEntry -> 1
                    o1 is PsiClass && o2 is PsiClass -> baseComparator.compare(o1, o2)
                    else -> 0
                }
            }
        }

        override fun getIconFlags() = 0

        override fun getElementText(element: PsiElement?): String? {
            return when (element) {
                is KtEnumEntry -> element.name
                is PsiClass -> psiClassRenderer.getElementText(element)
                else -> null
            }
        }

        override fun getContainerText(element: PsiElement?, name: String?): String? {
            return when (element) {
                is KtEnumEntry -> element.containingClassOrObject?.fqName?.asString()
                is PsiClass -> PsiClassListCellRenderer.getContainerTextStatic(element)
                else -> null
            }
        }
    }

    override fun applyTo(element: KtNamedDeclaration, editor: Editor?) {
        if (editor == null) throw IllegalArgumentException("This intention requires an editor")
        val project = element.project

        val classesToProcess = project.runSynchronouslyWithProgress(
                CodeInsightBundle.message("intention.implement.abstract.method.searching.for.descendants.progress"),
                true
        ) { findClassesToProcess(element).toList() } ?: return
        if (classesToProcess.isEmpty()) return

        classesToProcess.singleOrNull()?.let { return implementInClass(element, listOf(it)) }

        val renderer = ClassRenderer()
        val sortedClasses = classesToProcess.sortedWith(renderer.comparator)
        if (ApplicationManager.getApplication().isUnitTestMode) return implementInClass(element, sortedClasses)

        val list = JBList(sortedClasses).apply {
            selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
            cellRenderer = renderer
        }
        val builder = PopupChooserBuilder(list)
        renderer.installSpeedSearch(builder)
        builder
                .setTitle(CodeInsightBundle.message("intention.implement.abstract.method.class.chooser.title"))
                .setItemChoosenCallback {
                    val index = list.selectedIndex
                    if (index < 0) return@setItemChoosenCallback
                    @Suppress("UNCHECKED_CAST")
                    implementInClass(element, list.selectedValues.toList() as List<KtClassOrObject>)
                }
                .createPopup()
                .showInBestPositionFor(editor)
    }
}

class ImplementAbstractMemberIntention : ImplementAbstractMemberIntentionBase() {
    override fun computeText(element: KtNamedDeclaration): String? {
        return when(element) {
            is KtProperty -> "Implement abstract property"
            is KtNamedFunction -> "Implement abstract function"
            else -> null
        }
    }

    override fun acceptSubClass(subClassDescriptor: ClassDescriptor, memberDescriptor: CallableMemberDescriptor): Boolean {
        return subClassDescriptor.kind != ClassKind.INTERFACE && findExistingImplementation(subClassDescriptor, memberDescriptor) == null
    }

    override val preferConstructorParameters: Boolean
        get() = false
}

class ImplementAbstractMemberAsConstructorParameterIntention : ImplementAbstractMemberIntentionBase() {
    override fun computeText(element: KtNamedDeclaration): String? {
        if (element !is KtProperty) return null
        return "Implement as constructor parameter"
    }

    override fun acceptSubClass(subClassDescriptor: ClassDescriptor, memberDescriptor: CallableMemberDescriptor): Boolean {
        val kind = subClassDescriptor.kind
        return (kind == ClassKind.CLASS || kind == ClassKind.ENUM_CLASS)
               && subClassDescriptor !is JavaClassDescriptor
               && findExistingImplementation(subClassDescriptor, memberDescriptor) == null
    }

    override val preferConstructorParameters: Boolean
        get() = true

    override fun applicabilityRange(element: KtNamedDeclaration): TextRange? {
        if (element !is KtProperty) return null
        return super.applicabilityRange(element)
    }
}