/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.fir.declarations.FirResolvedTypeParameter
import org.jetbrains.kotlin.fir.descriptors.ConeTypeParameterDescriptor
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.name.Name

class ConeTypeParameterDescriptorImpl(
    override val symbol: ConeTypeParameterSymbol
) : ConeTypeParameterDescriptor, AbstractFirBasedDescriptor<FirResolvedTypeParameter>()