// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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