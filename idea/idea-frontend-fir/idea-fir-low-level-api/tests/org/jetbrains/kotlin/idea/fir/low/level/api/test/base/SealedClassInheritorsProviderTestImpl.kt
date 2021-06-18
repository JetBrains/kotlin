/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.test.base

import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProvider
import org.jetbrains.kotlin.fir.declarations.SealedClassInheritorsProviderInternals
import org.jetbrains.kotlin.fir.declarations.sealedInheritorsAttr
import org.jetbrains.kotlin.name.ClassId

internal class SealedClassInheritorsProviderTestImpl : SealedClassInheritorsProvider() {
    @OptIn(SealedClassInheritorsProviderInternals::class)
    override fun getSealedClassInheritors(firClass: FirRegularClass): List<ClassId> {
        return firClass.sealedInheritorsAttr ?: emptyList()
    }
}