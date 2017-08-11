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

package org.jetbrains.kotlin.idea.refactoring.safeDelete

import com.intellij.psi.PsiElement
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

class SafeDeleteValueArgumentListUsageInfo(
        parameter: PsiElement,
        vararg valueArguments: KtValueArgument
) : SafeDeleteReferenceSimpleDeleteUsageInfo(valueArguments.first(), parameter, true) {
    private val valueArgumentPointers = valueArguments.map { it.createSmartPointer() }

    override fun deleteElement() {
        for (valueArgumentPointer in valueArgumentPointers) {
            val valueArgument = valueArgumentPointer.element ?: return
            val parent = valueArgument.parent
            if (parent is KtValueArgumentList) {
                parent.removeArgument(valueArgument)
            }
            else {
                valueArgument.delete()
            }
        }
    }
}
