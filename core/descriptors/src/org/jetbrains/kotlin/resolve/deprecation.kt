/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor

interface Deprecation {
    val deprecationLevel: DeprecationLevelValue
    val message: String?
    val target: DeclarationDescriptor
}

// values from kotlin.DeprecationLevel
enum class DeprecationLevelValue {
    WARNING, ERROR, HIDDEN
}

val DEPRECATED_FUNCTION_KEY = object : CallableDescriptor.UserDataKey<Deprecation> {}
