/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaWhenMissingCase
import org.jetbrains.kotlin.diagnostics.WhenMissingCase

internal fun WhenMissingCase.toKaWhenMissingCase(): KaWhenMissingCase = when (this) {
    is WhenMissingCase.Unknown -> KaWhenMissingCase.UnknownCase
    is WhenMissingCase.ConditionTypeIsExpect.SealedClass -> KaWhenMissingCase.ExpectTypeCase.ExpectSealedClassCase
    is WhenMissingCase.ConditionTypeIsExpect.SealedInterface -> KaWhenMissingCase.ExpectTypeCase.ExpectSealedInterfaceCase
    is WhenMissingCase.ConditionTypeIsExpect.Enum -> KaWhenMissingCase.ExpectTypeCase.ExpectEnumCase
    is WhenMissingCase.NullIsMissing -> KaWhenMissingCase.NullCase
    is WhenMissingCase.BooleanIsMissing.TrueIsMissing -> KaWhenMissingCase.BooleanCase(true)
    is WhenMissingCase.BooleanIsMissing.FalseIsMissing -> KaWhenMissingCase.BooleanCase(false)
    is WhenMissingCase.IsTypeCheckIsMissing -> KaWhenMissingCase.TypeCase(classId, isSingleton, ownTypeParametersCount)
    is WhenMissingCase.EnumCheckIsMissing -> KaWhenMissingCase.EnumEntryCase(callableId)
}
