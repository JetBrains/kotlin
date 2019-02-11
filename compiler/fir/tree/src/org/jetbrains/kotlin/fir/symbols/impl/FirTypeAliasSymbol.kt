/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeAliasSymbol
import org.jetbrains.kotlin.name.ClassId

class FirTypeAliasSymbol(override val classId: ClassId) : ConeTypeAliasSymbol, AbstractFirBasedSymbol<FirTypeAlias>()