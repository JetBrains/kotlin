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
package com.intellij.debugger.streams.kotlin.psi

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.KotlinType

/**
 * @author Vitaliy.Bibaev
 */

fun KtExpression.resolveType(): KotlinType =
    this.analyze().getType(this)!!

fun KtValueArgument.resolveType(): KotlinType = getArgumentExpression()!!.resolveType()

fun KotlinType.getPackage(withGenerics: Boolean): String = StringUtil.getPackageName(getJetTypeFqName(withGenerics))

fun KtCallExpression.callName(): String = this.calleeExpression!!.text

fun KtCallExpression.receiverType(): KotlinType? {
  val resolvedCall = getResolvedCall(analyze())
  return resolvedCall?.dispatchReceiver?.type
}