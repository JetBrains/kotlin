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

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetConstructorDelegationCall
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetSecondaryConstructor

public class JetConstructorDelegationCallUsage(call: JetConstructorDelegationCall) : JetUsageInfo<JetConstructorDelegationCall>(call) {
    override fun processUsage(changeInfo: JetChangeInfo, element: JetConstructorDelegationCall): Boolean {
        val isThisCall = element.getCalleeExpression()!!.isThis()

        val psiFactory = JetPsiFactory(element)
        var elementToWorkWith = element
        if (changeInfo.getNewParametersCount() > 0 && element.getCalleeExpression()!!.isEmpty()) {
            val delegationKindName = if (isThisCall) "this" else "super"
            elementToWorkWith =
                    element.replace(psiFactory.createConstructorDelegationCall("$delegationKindName()")) as JetConstructorDelegationCall
            elementToWorkWith.getParent()!!.addBefore(psiFactory.createColon(), elementToWorkWith)
        }

        val result = JetFunctionCallUsage(
                elementToWorkWith, changeInfo.methodDescriptor.originalPrimaryFunction).processUsage(changeInfo, elementToWorkWith)

        if (changeInfo.getNewParametersCount() == 0 && !isThisCall && !elementToWorkWith.getCalleeExpression()!!.isEmpty()) {
            (elementToWorkWith.getParent() as? JetSecondaryConstructor)?.getColon()?.delete()
            elementToWorkWith.replace(psiFactory.createConstructorDelegationCall(""))
        }

        return result
    }
}