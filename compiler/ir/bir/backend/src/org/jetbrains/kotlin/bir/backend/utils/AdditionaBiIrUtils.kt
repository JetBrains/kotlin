/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.utils

import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.declarations.BirClass
import org.jetbrains.kotlin.bir.declarations.BirConstructor
import org.jetbrains.kotlin.bir.declarations.BirFunction
import org.jetbrains.kotlin.bir.declarations.BirSymbolOwner
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.util.constructedClass

context(BirBackendContext)
fun BirClassSymbol?.isArrayOrPrimitiveArray(): Boolean =
    this == birBuiltIns.arrayClass
            || this in birBuiltIns.primitiveArraysToPrimitiveTypes

// Constructors can't be marked as inline in metadata, hence this check.
context(BirBackendContext)
fun BirFunction.isInlineArrayConstructor(): Boolean =
    this is BirConstructor && valueParameters.size == 2 && constructedClass.symbol.isArrayOrPrimitiveArray()