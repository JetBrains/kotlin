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

package org.jetbrains.kotlin.resolve.calls

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall


abstract class AbstractResolvedConstructorDelegationCallsTests : AbstractResolvedCallsTest() {
    override fun buildCachedCallAtIndex(
            bindingContext: BindingContext, jetFile: KtFile, index: Int
    ): Pair<PsiElement?, ResolvedCall<out CallableDescriptor>?> {
        val element = jetFile.findElementAt(index)
        val constructor = element?.getNonStrictParentOfType<KtSecondaryConstructor>()!!
        val delegationCall = constructor.getDelegationCall()

        val cachedCall = delegationCall.getParentResolvedCall(bindingContext, strict = false)
        return Pair(delegationCall, cachedCall)
    }
}
