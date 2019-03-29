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
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.isJvm

private val annotationFqName = FqName("kotlin.jvm.JvmOverloads")

class AddJvmOverloadsIntention : SelfTargetingIntention<KtModifierListOwner>(
        KtModifierListOwner::class.java, "Add '@JvmOverloads' annotation"
), LowPriorityAction {

    override fun isApplicableTo(element: KtModifierListOwner, caretOffset: Int): Boolean {
        val (targetName, parameters) = when (element) {
            is KtNamedFunction -> {
                val funKeyword = element.funKeyword ?: return false
                val valueParameterList = element.valueParameterList ?: return false
                if (caretOffset !in funKeyword.startOffset..valueParameterList.endOffset) {
                    return false
                }

                "function '${element.name}'" to valueParameterList.parameters
            }
            is KtSecondaryConstructor -> {
                val constructorKeyword = element.getConstructorKeyword()
                val valueParameterList = element.valueParameterList ?: return false
                if (caretOffset !in constructorKeyword.startOffset..valueParameterList.endOffset) {
                    return false
                }

                "secondary constructor" to valueParameterList.parameters
            }
            is KtPrimaryConstructor -> {
                val parameters = (element.valueParameterList ?: return false).parameters

                // For primary constructors with all default values, a zero-arg constructor is generated anyway. If there's only one
                // parameter and it has a default value, the bytecode with and without @JvmOverloads is exactly the same.
                if (parameters.singleOrNull()?.hasDefaultValue() == true) {
                    return false
                }

                "primary constructor" to parameters
            }
            else -> return false
        }

        text = "Add '@JvmOverloads' annotation to $targetName"

        return TargetPlatformDetector.getPlatform(element.containingKtFile).isJvm()
               && parameters.any { it.hasDefaultValue() }
               && element.findAnnotation(annotationFqName) == null
    }

    override fun applyTo(element: KtModifierListOwner, editor: Editor?) {
        if (element is KtPrimaryConstructor) {
            if (element.getConstructorKeyword() == null) {
                element.addBefore(KtPsiFactory(element).createConstructorKeyword(), element.valueParameterList)
            }
            element.addAnnotation(annotationFqName, whiteSpaceText = " ")
        }
        else {
            element.addAnnotation(annotationFqName)
        }
    }
}
