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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*

private val annotationFqName = FqName("kotlin.jvm.JvmOverloads")

class AddJvmOverloadsIntention : SelfTargetingIntention<KtParameterList>(
        KtParameterList::class.java, "Add '@JvmOverloads' annotation"
), LowPriorityAction {

    override fun isApplicableTo(element: KtParameterList, caretOffset: Int): Boolean {
        val parent = element.parent as? KtModifierListOwner ?: return false
        val target = when (parent) {
            is KtNamedFunction -> "function '${parent.name}'"
            is KtPrimaryConstructor -> "primary constructor"
            is KtSecondaryConstructor -> "secondary constructor"
            else -> return false
        }
        text = "Add '@JvmOverloads' annotation to $target"

        return !ProjectStructureUtil.isJsKotlinModule(element.getContainingKtFile())
               && element.parameters.any { it.hasDefaultValue() }
               && parent.findAnnotation(annotationFqName) == null
    }

    override fun applyTo(element: KtParameterList, editor: Editor?) {
        val parent = element.parent as KtModifierListOwner

        if (parent is KtPrimaryConstructor && parent.getConstructorKeyword() == null) {
            val keyword = KtPsiFactory(parent).createConstructorKeyword()
            parent.addBefore(keyword, element)
            parent.addAnnotation(annotationFqName, whiteSpaceText = " ")
        }
        else {
            parent.addAnnotation(annotationFqName)
        }
    }

}