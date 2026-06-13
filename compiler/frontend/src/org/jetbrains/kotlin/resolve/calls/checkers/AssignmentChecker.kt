/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.psi.KtBinaryExpression

@K1Deprecation
interface AssignmentChecker {

    fun check(assignmentExpression: KtBinaryExpression, context: CallCheckerContext)
}