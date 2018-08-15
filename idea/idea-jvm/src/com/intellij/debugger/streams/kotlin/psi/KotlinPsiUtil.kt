// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType

/**
 * @author Vitaliy.Bibaev
 */

object KotlinPsiUtil {
  private val WITH_TYPES_RENDERER = DescriptorRenderer.withOptions { modifiers = DescriptorRendererModifier.ALL }

  fun getTypeName(type: KotlinType): String = WITH_TYPES_RENDERER.renderType(type)

  fun getTypeWithoutTypeParameters(type: KotlinType): String {
    val descriptor = type.constructor.declarationDescriptor ?: return getTypeName(type)
    return descriptor.fqNameSafe.asString()
  }
}

fun KtExpression.resolveType(): KotlinType =
    this.analyze().getType(this)!!

fun KtCallExpression.callName(): String = this.calleeExpression!!.text

fun KtCallExpression.receiver(): ReceiverValue? {
  val resolvedCall = getResolvedCall(analyze()) ?: return null
  return resolvedCall.dispatchReceiver ?: resolvedCall.extensionReceiver
}

fun KtCallExpression.previousCall(): KtCallExpression? {
  val parent = this.parent as? KtDotQualifiedExpression ?: return null
  val receiverExpression = parent.receiverExpression
  if (receiverExpression is KtCallExpression) return receiverExpression
  if (receiverExpression is KtDotQualifiedExpression) return receiverExpression.selectorExpression as? KtCallExpression
  return null
}

fun KtCallExpression.receiverType(): KotlinType? = receiver()?.type
