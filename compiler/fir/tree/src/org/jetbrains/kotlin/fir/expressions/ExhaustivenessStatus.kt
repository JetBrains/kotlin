/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.name.ClassId

sealed class ExhaustivenessStatus {
    object Exhaustive : ExhaustivenessStatus()
    class NotExhaustive(val reasons: List<WhenMissingCase>) : ExhaustivenessStatus()
}

sealed class WhenMissingCase {
    object Unknown : WhenMissingCase()
    object NullIsMissing : WhenMissingCase()
    class BooleanIsMissing(val value: Boolean) : WhenMissingCase()
    class IsTypeCheckIsMissing(val classId: ClassId) : WhenMissingCase()
    class EnumCheckIsMissing(val classId: ClassId) : WhenMissingCase()
}

val FirWhenExpression.isExhaustive: Boolean
    get() = exhaustivenessStatus == ExhaustivenessStatus.Exhaustive
