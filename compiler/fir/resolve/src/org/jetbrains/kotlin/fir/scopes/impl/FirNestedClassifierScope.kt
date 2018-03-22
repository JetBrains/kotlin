/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.fir.symbols.toSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirNestedClassifierScope(val classId: ClassId, val session: FirSession) : FirScope {

    private val firProvider = FirProvider.getInstance(session)

    private fun ClassId.getFir(): FirMemberDeclaration? {
        return firProvider.getFirClassifierByFqName(this)
    }

    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (ConeSymbol) -> Boolean
    ): Boolean {
        val child = ClassId(classId.packageFqName, classId.relativeClassName.child(name), false)
        return child.getFir() == null || processor(child.toSymbol())
    }
}
