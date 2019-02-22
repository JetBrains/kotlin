/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.Name

class FirJavaClass(
    session: FirSession,
    symbol: FirClassSymbol,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    classKind: ClassKind,
    isStatic: Boolean
) : FirClassImpl(
    session, psi = null, symbol = symbol, name = name,
    visibility = visibility, modality = modality,
    isExpect = false, isActual = false, classKind = classKind, isInner = !isStatic,
    isCompanion = false, isData = false, isInline = false
)