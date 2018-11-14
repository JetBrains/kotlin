/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.scopes.FirTypeParameterScope

class FirClassLikeTypeParameterScope(classLike: FirMemberDeclaration) : FirTypeParameterScope {
    override val typeParameters = classLike.typeParameters.groupBy { it.name }
}