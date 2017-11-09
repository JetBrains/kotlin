/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.incremental

import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
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