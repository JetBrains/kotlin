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

import com.google.common.collect.Lists
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.actions.JetAddFunctionToClassifierAction
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import java.util.*

class AddFunctionToSupertypeFix(element: JetNamedFunction) : JetHintAction<JetNamedFunction>(element) {
    private val functionsToAdd: List<FunctionDescriptor> = generateFunctionsToAdd(element)

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        return super.isAvailable(project, editor, file) && !functionsToAdd.isEmpty()
    }

    override fun showHint(editor: Editor) = false

    override fun getText(): String {
        if (functionsToAdd.size == 1) {
            val newFunction = functionsToAdd.get(0)
            val supertype = newFunction.containingDeclaration as ClassDescriptor
            return JetBundle.message("add.function.to.type.action.single",
                    IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(newFunction),
                    supertype.name.toString())
        }
        else {
            return JetBundle.message("add.function.to.supertype.action.multiple")
        }
    }

    override fun getFamilyName() = JetBundle.message("add.function.to.supertype.family")

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        CommandProcessor.getInstance().runUndoTransparentAction(object : Runnable {
            override fun run() {
                createAction(project, editor).execute()
            }
        })
    }

    private fun createAction(project: Project, editor: Editor?): JetAddFunctionToClassifierAction {
        return JetAddFunctionToClassifierAction(project, editor, functionsToAdd)
    }

    companion object {

        private fun generateFunctionsToAdd(functionElement: JetNamedFunction): List<FunctionDescriptor> {
            val functionDescriptor = functionElement.resolveToDescriptor() as FunctionDescriptor

            val containingClass = functionDescriptor.containingDeclaration as? ClassDescriptor ?: return emptyList()

            val functions = ArrayList<FunctionDescriptor>()
            // TODO: filter out impossible supertypes (for example when argument's type isn't visible in a superclass).
            for (supertypeDescriptor in getSupertypes(containingClass)) {
                if (KotlinBuiltIns.isAnyOrNullableAny(supertypeDescriptor.defaultType)) continue
                functions.add(generateFunctionSignatureForType(functionDescriptor, supertypeDescriptor))
            }
            return functions
        }

        private fun getSupertypes(classDescriptor: ClassDescriptor): List<ClassDescriptor> {
            val supertypes = Lists.newArrayList(TypeUtils.getAllSupertypes(classDescriptor.defaultType))
            Collections.sort(supertypes, object : Comparator<JetType> {
                override fun compare(o1: JetType, o2: JetType): Int {
                    if (o1 == o2) {
                        return 0
                    }
                    if (JetTypeChecker.DEFAULT.isSubtypeOf(o1, o2)) {
                        return -1
                    }
                    if (JetTypeChecker.DEFAULT.isSubtypeOf(o2, o1)) {
                        return 1
                    }
                    return o1.toString().compareTo(o2.toString())
                }
            })
            val supertypesDescriptors = Lists.newArrayList<ClassDescriptor>()
            for (supertype in supertypes) {
                supertypesDescriptors.add(DescriptorUtils.getClassDescriptorForType(supertype))
            }
            return supertypesDescriptors
        }

        private fun generateFunctionSignatureForType(
                functionDescriptor: FunctionDescriptor,
                typeDescriptor: ClassDescriptor): FunctionDescriptor {
            // TODO: support for generics.
            var modality = typeDescriptor.modality
            if (typeDescriptor.kind == ClassKind.INTERFACE) {
                modality = Modality.OPEN
            }
            return functionDescriptor.copy(
                    typeDescriptor,
                    modality,
                    functionDescriptor.visibility,
                    CallableMemberDescriptor.Kind.DECLARATION,
                    /* copyOverrides = */ false)
        }

        fun createFactory(): JetIntentionActionsFactory {
            return object : JetSingleIntentionActionFactory() {
                public override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val function = QuickFixUtil.getParentElementOfType(diagnostic, JetNamedFunction::class.java)
                    return if (function == null) null else AddFunctionToSupertypeFix(function)
                }
            }
        }
    }
}
