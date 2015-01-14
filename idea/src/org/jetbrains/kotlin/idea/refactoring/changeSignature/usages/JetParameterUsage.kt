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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo

public class JetParameterUsage(
        element: JetSimpleNameExpression,
        private val parameterInfo: JetParameterInfo,
        private val containingFunction: JetFunctionDefinitionUsage<*>
) : JetUsageInfo<JetSimpleNameExpression>(element) {
    override fun processUsage(changeInfo: JetChangeInfo, element: JetSimpleNameExpression): Boolean {
        val newName = parameterInfo.getInheritedName(containingFunction)
        element.replace(JetPsiFactory(element.getProject()).createSimpleName(newName))
        return false
    }
}
