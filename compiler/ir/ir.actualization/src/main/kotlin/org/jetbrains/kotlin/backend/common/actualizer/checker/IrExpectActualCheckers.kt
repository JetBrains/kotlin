/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer.checker

internal object IrExpectActualCheckers {
    private val checkers: Set<IrExpectActualChecker> = setOf(
        IrAnnotationMatchingKmpChecker,
        IrAnnotationConflictingDefaultArgumentValueKmpChecker,
        IrKotlinActualAnnotationOnJavaKmpChecker,
        IrJavaDirectActualizationDefaultParametersInExpectKmpChecker,
        IrJavaDirectActualizationDefaultParametersInActualKmpChecker,
    )

    fun check(context: IrExpectActualChecker.Context) {
        for (checker in checkers) {
            checker.check(context)
        }
    }
}
