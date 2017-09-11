/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction

class AddJvmStaticIntention : SelfTargetingIntention<KtNamedFunction>(
    KtNamedFunction::class.java,
    "Add '@JvmStatic' annotation"
), LowPriorityAction {

    private val annotationFqName = FqName("kotlin.jvm.JvmStatic")

    override fun isApplicableTo(element: KtNamedFunction, caretOffset: Int): Boolean {
        if (element.findAnnotation(annotationFqName) != null) return false
        if (element.isTopLevel) return false
        val detector = MainFunctionDetector { function ->
            function.resolveToDescriptorIfAny() as? FunctionDescriptor
        }
        return detector.isMain(element, false)
    }

    override fun applyTo(element: KtNamedFunction, editor: Editor?) {
        element.addAnnotation(annotationFqName)
    }
}
