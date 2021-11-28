/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.isTopLevelInPackage
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.constants.IntegerLiteralTypeConstructor
import org.jetbrains.kotlin.types.KotlinType

class SynchronizedByValueChecker : CallChecker {
    private fun KotlinType.isValueOrPrimitive(): Boolean = KotlinBuiltIns.isPrimitiveType(this)
            || constructor.declarationDescriptor.let { it is ClassDescriptor && it.isValue }
            || constructor.let { manyTypes -> manyTypes is IntegerLiteralTypeConstructor && manyTypes.possibleTypes.any { it.isValueOrPrimitive() } }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (resolvedCall.resultingDescriptor?.isTopLevelInPackage("synchronized", "kotlin") != true) return
        val argument = resolvedCall.valueArgumentsByIndex?.get(0)?.arguments?.firstOrNull() ?: return
        val type = argument.getArgumentExpression()?.getType(context.trace.bindingContext) ?: return
        if (type.isValueOrPrimitive()) {
            context.trace.report(Errors.FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES.on(reportOn, type))
        }
    }
}