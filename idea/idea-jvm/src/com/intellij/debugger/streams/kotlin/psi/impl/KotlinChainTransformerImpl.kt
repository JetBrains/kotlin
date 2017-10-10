/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.debugger.streams.kotlin.psi.impl

import com.intellij.debugger.streams.kotlin.psi.callName
import com.intellij.debugger.streams.kotlin.psi.getPackage
import com.intellij.debugger.streams.kotlin.psi.receiverType
import com.intellij.debugger.streams.kotlin.psi.resolveType
import com.intellij.debugger.streams.psi.*
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes
import com.intellij.debugger.streams.wrapper.CallArgument
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.debugger.streams.wrapper.impl.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * @author Vitaliy.Bibaev
 */
class KotlinChainTransformerImpl : ChainTransformer<KtCallExpression> {
  override fun transform(callChain: List<KtCallExpression>, context: PsiElement): StreamChain {
    val firstCall = callChain.first()
    val parent = firstCall.parent
    val qualifier = if (parent is KtDotQualifiedExpression)
      QualifierExpressionImpl(parent.receiverExpression.text, parent.receiverExpression.textRange, KotlinTypes.ANY)
    else
      QualifierExpressionImpl("", TextRange.EMPTY_RANGE, KotlinTypes.ANY) // This possible only when this inherits Stream

    val intermediateCalls = mutableListOf<IntermediateStreamCall>()
    for (call in callChain.subList(0, callChain.size - 1)) {
      intermediateCalls += IntermediateStreamCallImpl(call.callName(),
          call.valueArguments.map { it.toCallArgument() },
          KotlinTypes.ANY, KotlinTypes.ANY,
          call.textRange)
    }

    val terminationsPsiCall = callChain.last()
    // TODO: infer true types
    val terminationCall = TerminatorStreamCallImpl(terminationsPsiCall.callName(), emptyList(),
        KotlinTypes.ANY, KotlinTypes.ANY,
        terminationsPsiCall.textRange)

    return StreamChainImpl(qualifier, intermediateCalls, terminationCall, context)
  }

  private fun KtValueArgument.toCallArgument(): CallArgument {
    val argExpression = getArgumentExpression()!!
    return CallArgumentImpl(argExpression.resolveType().getJetTypeFqName(true), argExpression.text)
  }
}
