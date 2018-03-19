/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.descriptors

import org.jetbrains.kotlin.fir.UnambiguousFqName
import org.jetbrains.kotlin.fir.types.ConeKotlinType


interface ConeDescriptor

interface ConeClassifierDescriptor : ConeDescriptor

interface ConeClassifierDescriptorWithTypeParameters : ConeClassifierDescriptor {
    val typeParameters: List<ConeTypeParameterDescriptor>

    val fqName: UnambiguousFqName
}

interface ConeTypeParameterDescriptor : ConeClassifierDescriptor

interface ConeTypeAliasDescriptor : ConeClassifierDescriptorWithTypeParameters {
    val expandedType: ConeKotlinType
}

interface ConeClassDescriptor : ConeClassifierDescriptorWithTypeParameters {
    val superTypes: List<ConeKotlinType>
    val nestedClassifiers: List<ConeClassifierDescriptor>
}
