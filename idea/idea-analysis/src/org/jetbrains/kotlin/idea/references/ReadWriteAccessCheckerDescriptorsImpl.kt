/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.types.expressions.OperatorConventions

class ReadWriteAccessCheckerDescriptorsImpl : ReadWriteAccessChecker {
    override fun readWriteAccessWithFullExpressionByResolve(assignment: KtBinaryExpression): Pair<ReferenceAccess, KtExpression> {
        val resolvedCall = assignment.resolveToCall() ?: return ReferenceAccess.READ_WRITE to assignment
        if (!resolvedCall.isReallySuccess()) return ReferenceAccess.READ_WRITE to assignment
        return if (resolvedCall.resultingDescriptor.name in OperatorConventions.ASSIGNMENT_OPERATIONS.values)
            ReferenceAccess.READ to assignment
        else
            ReferenceAccess.READ_WRITE to assignment
    }
}