/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.calls.components.stableType
import org.jetbrains.kotlin.resolve.calls.model.ExpressionKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.NewAbstractResolvedCall
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.KotlinType

class SynchronizedByValueChecker : CallChecker {
    private fun KotlinType.isValueOrPrimitive(): Boolean = KotlinBuiltIns.isPrimitiveType(this)
            || constructor.declarationDescriptor.let { it is ClassDescriptor && it.isValue }
            || constructor.let { manyTypes -> manyTypes is IntegerLiteralTypeConstructor && manyTypes.possibleTypes.any { it.isValueOrPrimitive() } }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (resolvedCall.resultingDescriptor?.fqNameOrNull()?.render() != "kotlin.synchronized") return
        val argumentMappingByOriginal = (resolvedCall as? NewAbstractResolvedCall)?.resolvedCallAtom?.argumentMappingByOriginal ?: return
        val callArgument = argumentMappingByOriginal.entries.firstOrNull { it.key.name.asString() == "lock" }?.value ?: return
        val lockArgument = callArgument.arguments.firstOrNull() as? ExpressionKotlinCallArgument ?: return
        val type = lockArgument.receiver.stableType
        if (type.isValueOrPrimitive()) {
            context.trace.report(Errors.FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES.on(reportOn, type))
        }
    }
}