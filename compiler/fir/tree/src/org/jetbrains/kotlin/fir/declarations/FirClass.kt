/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.types.FirType

interface FirClass : FirDeclarationContainer, FirMemberDeclaration {
    // including delegated types
    val superTypes: List<FirType>

    val classKind: ClassKind

    val isCompanion: Boolean

    val isData: Boolean
}