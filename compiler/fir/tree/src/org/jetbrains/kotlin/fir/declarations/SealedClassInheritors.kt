/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.name.ClassId


@RequiresOptIn("For getting/setting sealed class inheritors, consider using getSealedClassInheritors/setSealedClassInheritors")
annotation class SealedClassInheritorsProviderInternals

abstract class SealedClassInheritorsProvider : FirSessionComponent {
    abstract fun getSealedClassInheritors(firClass: FirRegularClass): List<ClassId>
}

private val FirSession.sealedClassInheritorsProvider: SealedClassInheritorsProvider by FirSession.sessionComponentAccessor()

object SealedClassInheritorsProviderImpl : SealedClassInheritorsProvider() {
    @OptIn(SealedClassInheritorsProviderInternals::class)
    override fun getSealedClassInheritors(firClass: FirRegularClass): List<ClassId> {
        return firClass.sealedInheritorsAttr?.value ?: emptyList()
    }
}


fun FirRegularClass.getSealedClassInheritors(session: FirSession): List<ClassId> {
    require(this.isSealed)
    return session.sealedClassInheritorsProvider.getSealedClassInheritors(this)
}

@OptIn(SealedClassInheritorsProviderInternals::class)
fun FirRegularClass.setSealedClassInheritors(inheritors: List<ClassId>) {
    require(this.isSealed)
    sealedInheritorsAttr = lazyOf(inheritors.sortedBy { it.asFqNameString() })
}

@OptIn(SealedClassInheritorsProviderInternals::class)
fun FirRegularClass.setSealedClassInheritors(inheritorComputer: () -> List<ClassId>) {
    require(this.isSealed)
    sealedInheritorsAttr = lazy { inheritorComputer().sortedBy { it.asFqNameString() } }
}

private object SealedClassInheritorsKey : FirDeclarationDataKey()

@SealedClassInheritorsProviderInternals
var FirRegularClass.sealedInheritorsAttr: Lazy<List<ClassId>>? by FirDeclarationDataRegistry.data(SealedClassInheritorsKey)
    private set
