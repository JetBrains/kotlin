/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.type

import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.type.AbstractAtomicReferenceToPrimitiveTypeChecker
import org.jetbrains.kotlin.name.JvmStandardClassIds

object FirJvmAtomicReferenceToPrimitiveTypeChecker :
    AbstractAtomicReferenceToPrimitiveTypeChecker(JvmStandardClassIds.ATOMIC_REFERENCE_CLASS_ID, MppCheckerKind.Platform)
