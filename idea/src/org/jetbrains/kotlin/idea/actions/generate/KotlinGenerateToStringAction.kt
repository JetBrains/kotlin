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

package org.jetbrains.kotlin.idea.actions.generate

import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.insertMembersAfter
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.core.util.DescriptorMemberChooserObject
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

private fun ClassDescriptor.findDeclaredToString(checkSupers: Boolean): FunctionDescriptor? {
    return findDeclaredFunction("toString", checkSupers) { it.valueParameters.isEmpty() && it.typeParameters.isEmpty() }
}

class KotlinGenerateToStringAction : KotlinGenerateMemberActionBase<KotlinGenerateToStringAction.Info>() {
    companion object {
        private val LOG = Logger.getInstance(KotlinGenerateToStringAction::class.java)

        var KtClass.adjuster: ((Info) -> Info)? by UserDataProperty(Key.create("ADJUSTER"))
    }

    data class Info(val classDescriptor: ClassDescriptor,
                    val variablesToUse: List<VariableDescriptor>,
                    val generateSuperCall: Boolean,
                    val generator: Generator,
                    val project: Project)

    enum class Generator(val text: String) {
        SINGLE_TEMPLATE("Single template") {
            override fun generate(info: Info): String {
                val className = info.classDescriptor.name.asString()

                return buildString {
                    append("return \"${className.quoteIfNeeded()}(")
                    info.variablesToUse.joinTo(this) {
                        val ref = (DescriptorToSourceUtilsIde.getAnyDeclaration(info.project, it) as PsiNameIdentifierOwner).nameIdentifier!!.text
                        "$ref=${renderVariableValue(it, ref)}"
                    }
                    append(")")
                    if (info.generateSuperCall) {
                        append(" \${super.toString()}")
                    }
                    append("\"")
                }
            }
        },

        MULTIPLE_TEMPLATES("Multiple templates with concatenation") {
            override fun generate(info: Info): String {
                val className = info.classDescriptor.name.asString()

                return buildString {
                    if (info.variablesToUse.isNotEmpty()) {
                        append("return \"${className.quoteIfNeeded()}(\" +\n")
                        val varIterator = info.variablesToUse.iterator()
                        while (varIterator.hasNext()) {
                            val it = varIterator.next()
                            val ref = (DescriptorToSourceUtilsIde.getAnyDeclaration(info.project, it) as PsiNameIdentifierOwner).nameIdentifier!!.text
                            append("\"$ref=${renderVariableValue(it, ref)}")
                            if (varIterator.hasNext()) {
                                append(',')
                            }
                            append("\" +\n")
                        }
                        append("\")\"")
                    }
                    else {
                        append("return \"$className()\"")
                    }

                    if (info.generateSuperCall) {
                        append(" +\n \" \${super.toString()}\"")
                    }
                }
            }
        };

        protected fun renderVariableValue(variableDescriptor: VariableDescriptor, ref: String): String {
            val type = variableDescriptor.type
            return when {
                KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type) -> "\${java.util.Arrays.toString($ref)}"
                KotlinBuiltIns.isString(type) -> "'$$ref'"
                else -> "$$ref"
            }
        }

        abstract fun generate(info: Info): String
    }

    override fun isValidForClass(targetClass: KtClassOrObject): Boolean {
        return targetClass is KtClass
               && !targetClass.isAnnotation()
               && !targetClass.isInterface()
    }

    override fun prepareMembersInfo(klass: KtClassOrObject, project: Project, editor: Editor?): Info? {
        if (klass !is KtClass) throw AssertionError("Not a class: ${klass.getElementTextWithContext()}")

        val context = klass.analyzeFully()
        val classDescriptor = context.get(BindingContext.CLASS, klass) ?: return null

        classDescriptor.findDeclaredToString(false)?.let {
            if (!confirmMemberRewrite(klass, it)) return null

            runWriteAction {
                try {
                    it.source.getPsi()?.delete()
                }
                catch(e: IncorrectOperationException) {
                    LOG.error(e)
                }
            }
        }

        val properties = getPropertiesToUseInGeneratedMember(klass)
        if (ApplicationManager.getApplication().isUnitTestMode) {
            val info = Info(classDescriptor,
                            properties.map { context[BindingContext.DECLARATION_TO_DESCRIPTOR, it] as VariableDescriptor },
                            false,
                            Generator.SINGLE_TEMPLATE,
                            project)
            return klass.adjuster?.let { it(info) } ?: info
        }

        val superToString = classDescriptor.getSuperClassOrAny().findDeclaredToString(true)!!

        val memberChooserObjects = properties.map { DescriptorMemberChooserObject(it, it.resolveToDescriptor()) }.toTypedArray()
        val headerPanel = ToStringMemberChooserHeaderPanel(!superToString.builtIns.isMemberOfAny(superToString))
        val chooser = MemberChooser<DescriptorMemberChooserObject>(memberChooserObjects, true, true, project, false, headerPanel).apply {
            title = "Generate toString()"
            setCopyJavadocVisible(false)
            selectElements(memberChooserObjects)
        }

        chooser.show()
        if (chooser.exitCode != DialogWrapper.OK_EXIT_CODE) return null

        return Info(classDescriptor,
                    chooser.selectedElements?.map { it.descriptor as VariableDescriptor } ?: emptyList(),
                    headerPanel.isGenerateSuperCall,
                    headerPanel.selectedGenerator,
                    project)
    }

    private fun generateToString(targetClass: KtClassOrObject, info: Info): KtNamedFunction? {
        val superToString = info.classDescriptor.getSuperClassOrAny().findDeclaredToString(true)!!
        return generateFunctionSkeleton(superToString, targetClass).apply {
            bodyExpression!!.replace(KtPsiFactory(targetClass).createExpression("{\n${info.generator.generate(info)}\n}"))
        }
    }

    override fun generateMembers(project: Project, editor: Editor?, info: Info): List<KtDeclaration> {
        val targetClass = info.classDescriptor.source.getPsi() as KtClass
        val prototype = generateToString(targetClass, info) ?: return emptyList()
        val anchor = with(targetClass.declarations) { lastIsInstanceOrNull<KtNamedFunction>() ?: lastOrNull() }
        return insertMembersAfter(editor, targetClass, listOf(prototype), anchor)
    }
}