/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.bir.BirDynamicPropertyKey
import org.jetbrains.kotlin.bir.GlobalBirDynamicProperty
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

object GlobalJvmBirDynamicProperties {
    val ClassNameOverride = GlobalBirDynamicProperty<_, JvmClassName>(BirClass)
}