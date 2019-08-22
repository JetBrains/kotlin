/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.ClassId

val FirTypeRef.isAny: Boolean get() = isBuiltinType(StandardClassIds.Any, false)
val FirTypeRef.isNullableAny: Boolean get() = isBuiltinType(StandardClassIds.Any, true)
val FirTypeRef.isNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, false)
val FirTypeRef.isNullableNothing: Boolean get() = isBuiltinType(StandardClassIds.Nothing, true)
val FirTypeRef.isUnit: Boolean get() = isBuiltinType(StandardClassIds.Unit, false)

private fun FirTypeRef.isBuiltinType(classId: ClassId, isNullable: Boolean): Boolean =
    this is FirImplicitBuiltinTypeRef && type.lookupTag.classId == classId && type.isNullable == isNullable