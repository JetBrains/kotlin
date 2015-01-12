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

package org.jetbrains.kotlin.idea.actions

import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.psi.PsiElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.psi.JetClassOrObject

public class KotlinQualifiedNameProvider: QualifiedNameProvider {
    override fun adjustElementToCopy(element: PsiElement?) = null

    override fun getQualifiedName(element: PsiElement?): String? {
        when (element) {
            is JetClassOrObject -> return element.getFqName()?.asString()
            is JetNamedFunction, is JetProperty -> {
                val descriptor = (element as JetDeclaration).resolveToDescriptor()
                val fqNameUnsafe = DescriptorUtils.getFqName(descriptor)
                if (fqNameUnsafe.isSafe()) {
                    return fqNameUnsafe.asString()
                }
            }
        }
        return null
    }

    override fun qualifiedNameToElement(fqn: String?, project: Project?) = null

    override fun insertQualifiedName(fqn: String?, element: PsiElement?, editor: Editor?, project: Project?) {
    }
}
