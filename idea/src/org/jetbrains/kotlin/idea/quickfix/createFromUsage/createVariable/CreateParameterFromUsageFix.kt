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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingContext

public class CreateParameterFromUsageFix(
        val functionDescriptor: FunctionDescriptor,
        val bindingContext: BindingContext,
        val parameterInfo: JetParameterInfo,
        val defaultValueContext: JetElement
) : CreateFromUsageFixBase(defaultValueContext) {
    override fun getText(): String {
        return JetBundle.message("create.parameter.from.usage", parameterInfo.getName())
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile?) {
        val config = object : JetChangeSignatureConfiguration {
            override fun configure(originalDescriptor: JetMethodDescriptor, bindingContext: BindingContext): JetMethodDescriptor {
                return originalDescriptor.modify { it.addParameter(parameterInfo) }
            }

            override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean = false
        }

        runChangeSignature(project, functionDescriptor, config, bindingContext, defaultValueContext, getText())
    }
}
