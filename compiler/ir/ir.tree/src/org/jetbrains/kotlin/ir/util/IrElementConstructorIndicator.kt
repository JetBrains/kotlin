/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

/**
A hack used to force compiler to resolve calls to a
constructor, instead of builder function with the same name.
To be removed soon.
 */
internal object IrElementConstructorIndicator