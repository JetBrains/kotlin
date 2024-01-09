/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.utils

import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.types.BirTypeSystemContext
import org.jetbrains.kotlin.bir.util.FqNameEqualityChecker
import org.jetbrains.kotlin.types.AbstractTypeChecker


fun BirClassifierSymbol.isSubtypeOfClass(superClass: BirClassSymbol): Boolean =
    FqNameEqualityChecker.areEqual(this, superClass) || isStrictSubtypeOfClass(superClass)

fun BirClassifierSymbol.isStrictSubtypeOfClass(superClass: BirClassSymbol): Boolean =
    superTypes.any { it.isSubtypeOfClass(superClass) }

fun BirType.isSubtypeOfClass(superClass: BirClassSymbol): Boolean =
    this is BirSimpleType && classifier.isSubtypeOfClass(superClass)

fun BirType.isStrictSubtypeOfClass(superClass: BirClassSymbol): Boolean =
    this is BirSimpleType && classifier.isStrictSubtypeOfClass(superClass)

fun BirType.isSubtypeOf(superType: BirType, typeSystem: BirTypeSystemContext): Boolean =
    AbstractTypeChecker.isSubtypeOf(createBirTypeCheckerState(typeSystem), this, superType)
