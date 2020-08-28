/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.name.Name

interface FirContainingNamesAwareScope {
    fun getCallableNames(): Set<Name>

    fun getClassifierNames(): Set<Name>
}

fun FirScope.getContainingCallableNamesIfPresent(): Set<Name> =
    if (this is FirContainingNamesAwareScope) getCallableNames() else emptySet()

fun FirScope.getContainingClassifierNamesIfPresent(): Set<Name> =
    if (this is FirContainingNamesAwareScope) getClassifierNames() else emptySet()
