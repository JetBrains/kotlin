/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.scopes.FirTypeParameterScope
import org.jetbrains.kotlin.name.Name

class FirMemberTypeParameterScope(callableMember: FirMemberDeclaration<*>) : FirTypeParameterScope() {
    override val typeParameters: Map<Name, List<FirTypeParameter>> =
        callableMember.typeParameters.filterIsInstance<FirTypeParameter>().groupBy { it.name }
}
