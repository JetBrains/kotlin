/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.fir.UnambiguousFqName
import org.jetbrains.kotlin.fir.declarations.FirResolvedClass
import org.jetbrains.kotlin.fir.descriptors.ConeClassDescriptor
import org.jetbrains.kotlin.fir.descriptors.ConeClassifierDescriptor
import org.jetbrains.kotlin.fir.descriptors.ConeTypeParameterDescriptor
import org.jetbrains.kotlin.fir.types.ConeKotlinType

class ConeClassDescriptorImpl(
    override val typeParameters: List<ConeTypeParameterDescriptor>,
    override val fqName: UnambiguousFqName,
    override val superTypes: List<ConeKotlinType>,
    override val nestedClassifiers: List<ConeClassifierDescriptor>
) : ConeClassDescriptor, AbstractFirBasedDescriptor<FirResolvedClass>() {
}