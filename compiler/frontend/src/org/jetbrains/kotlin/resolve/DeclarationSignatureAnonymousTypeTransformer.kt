/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.types.KotlinType

interface DeclarationSignatureAnonymousTypeTransformer {
    fun transformAnonymousType(descriptor: DeclarationDescriptorWithVisibility, type: KotlinType): KotlinType?
}