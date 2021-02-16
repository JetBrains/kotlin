/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.name.Name

val NAME_FOR_BACKING_FIELD = Name.identifier("field")
val NAME_FOR_DEFAULT_VALUE_PARAMETER = Name.identifier("value")
val CONSTRUCTOR_NAME = Name.special("<init>")

// Data class synthetic members
val COPY_NAME = Name.identifier("copy")
val EQUALS_NAME = Name.identifier("equals")
val HASHCODE_NAME = Name.identifier("hashCode")
val TOSTRING_NAME = Name.identifier("toString")
