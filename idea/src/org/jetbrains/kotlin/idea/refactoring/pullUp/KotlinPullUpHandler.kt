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

package org.jetbrains.kotlin.idea.refactoring.pullUp

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.lang.ElementsHandler
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.refactoring.canRefactor
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfoStorage
import org.jetbrains.kotlin.idea.refactoring.memberInfo.qualifiedNameForRendering
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.types.typeUtil.supertypes
import java.util.HashSet

public class KotlinPullUpHandler : RefactoringActionHandler, ElementsHandler {
    companion object {
        val PULLUP_TEST_HELPER_KEY = "PULLUP_TEST_HELPER_KEY"
    }

    interface TestHelper {
        fun adjustMembers(members: List<KotlinMemberInfo>): List<KotlinMemberInfo>
        fun chooseSuperClass(superClasses: List<JetClass>): JetClass
    }

    private fun reportWrongPosition(project: Project, editor: Editor?) {
        val message = RefactoringBundle.getCannotRefactorMessage(
                RefactoringBundle.message("the.caret.should.be.positioned.inside.a.class.to.pull.members.from")
        )
        CommonRefactoringUtil.showErrorHint(project, editor, message, PULL_MEMBERS_UP, "refactoring.pullMembersUp")
    }

    private fun reportWrongContext(project: Project, editor: Editor?) {
        val message = RefactoringBundle.getCannotRefactorMessage(
                RefactoringBundle.message("is.not.supported.in.the.current.context", PULL_MEMBERS_UP)
        )
        CommonRefactoringUtil.showErrorHint(project, editor, message, PULL_MEMBERS_UP, HelpID.MEMBERS_PULL_UP)
    }

    private fun reportNoSuperClasses(project: Project, editor: Editor?, classOrObject: JetClassOrObject) {
        val message = RefactoringBundle.getCannotRefactorMessage(
                RefactoringBundle.message("class.does.not.have.base.classes.interfaces.in.current.project",
                                          classOrObject.qualifiedNameForRendering())
        )
        CommonRefactoringUtil.showErrorHint(project, editor, message, PULL_MEMBERS_UP, HelpID.MEMBERS_PULL_UP)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        val offset = editor.caretModel.offset
        editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)

        val target = (file.findElementAt(offset) ?: return).parentsWithSelf.firstOrNull {
            it is JetClassOrObject || ((it is JetNamedFunction || it is JetProperty) && it.parent is JetClassBody)
        }

        if (target == null) {
            reportWrongPosition(project, editor)
            return
        }
        if (!target.canRefactor()) return

        invoke(project, arrayOf(target), dataContext)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        val element = elements.singleOrNull() ?: return

        val editor = dataContext?.let { CommonDataKeys.EDITOR.getData(it) }

        val (classOrObject, member) = when (element) {
            is JetNamedFunction, is JetProperty -> element.getStrictParentOfType<JetClassOrObject>() to element as JetNamedDeclaration?
            is JetClassOrObject -> element to null
            else -> {
                reportWrongPosition(project, editor)
                return
            }
        }

        invoke(project, editor, classOrObject, member, dataContext)
    }

    private fun invoke(project: Project,
                       editor: Editor?,
                       classOrObject: JetClassOrObject?,
                       member: JetNamedDeclaration?,
                       dataContext: DataContext?) {
        if (classOrObject == null) {
            reportWrongContext(project, editor)
            return
        }

        val classDescriptor = classOrObject.resolveToDescriptor() as ClassDescriptor
        val superClasses = classDescriptor.defaultType
                .supertypes()
                .asSequence()
                .map {
                    val descriptor = it.constructor.declarationDescriptor
                    val declaration = descriptor?.let { DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) }
                    if (declaration is JetClass && declaration.canRefactor()) declaration else null
                }
                .filterNotNull()
                .toSortedListBy { it.qualifiedNameForRendering() }

        if (superClasses.isEmpty()) {
            val containingClass = classOrObject.getStrictParentOfType<JetClassOrObject>()
            if (containingClass != null) {
                invoke(project, editor, containingClass, classOrObject, dataContext)
            }
            else {
                reportNoSuperClasses(project, editor, classOrObject)
            }
            return
        }

        val memberInfoStorage = KotlinMemberInfoStorage(classOrObject)
        val members = memberInfoStorage.getClassMemberInfos(classOrObject)

        if (ApplicationManager.getApplication().isUnitTestMode) {
            val helper = dataContext?.getData(PULLUP_TEST_HELPER_KEY) as TestHelper
            val selectedMembers = helper.adjustMembers(members)
            val targetClass = helper.chooseSuperClass(superClasses)
            checkConflicts(project, classOrObject, targetClass, selectedMembers) {
                KotlinPullUpDialog.createProcessor(classOrObject, targetClass, selectedMembers).run()
            }
        }
        else {
            val manager = classOrObject.manager
            members.filter { manager.areElementsEquivalent(it.member, member) }.forEach { it.isChecked = true }

            KotlinPullUpDialog(project, classOrObject, superClasses, memberInfoStorage).show()
        }
    }

    override fun isEnabledOnElements(elements: Array<out PsiElement>): Boolean {
        return elements.mapTo(HashSet<PsiElement>()) {
            when (it) {
                is JetNamedFunction, is JetProperty ->
                    (it.parent as? JetClassBody)?.parent as? JetClassOrObject
                is JetClassOrObject -> it
                else -> null
            } ?: return false
        }.size() == 1
    }
}

val PULL_MEMBERS_UP = "Pull Members Up"