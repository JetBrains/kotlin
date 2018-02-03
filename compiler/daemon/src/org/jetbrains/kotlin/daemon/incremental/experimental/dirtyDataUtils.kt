/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.incremental.experimental

import org.jetbrains.kotlin.daemon.common.experimental.SimpleDirtyData
import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.FqName

fun SimpleDirtyData.toDirtyData(): DirtyData {
    val dirtyClassesFqNames = dirtyClassesFqNames.map(::FqName)
    val dirtyLookupSymbols = dirtyLookupSymbols.map {
        LookupSymbol(scope = it.substringBeforeLast("."), name = it.substringAfterLast("."))
    }
    return DirtyData(dirtyLookupSymbols, dirtyClassesFqNames)
}

fun DirtyData.toSimpleDirtyData(): SimpleDirtyData {
    return SimpleDirtyData(dirtyClassesFqNames = dirtyClassesFqNames.map(FqName::asString),
                           dirtyLookupSymbols = dirtyLookupSymbols.map { "${it.scope}.${it.name}" })
}