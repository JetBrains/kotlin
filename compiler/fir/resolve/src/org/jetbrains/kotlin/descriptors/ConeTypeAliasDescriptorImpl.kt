/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.fir.declarations.FirResolvedTypeAlias
import org.jetbrains.kotlin.fir.descriptors.ConeTypeAliasDescriptor
import org.jetbrains.kotlin.fir.descriptors.ConeTypeParameterDescriptor
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.ClassId

class ConeTypeAliasDescriptorImpl(
    override val typeParameters: List<ConeTypeParameterDescriptor>,
    override val fqName: ClassId,
    override val expandedType: ConeKotlinType
) : ConeTypeAliasDescriptor, AbstractFirBasedDescriptor<FirResolvedTypeAlias>()