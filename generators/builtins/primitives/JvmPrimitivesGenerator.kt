/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import java.io.PrintWriter

class JvmPrimitivesGenerator(writer: PrintWriter) : BasePrimitivesGenerator(writer) {
    override fun ClassBuilder.modifyGeneratedClass(thisKind: PrimitiveType) {
        appendDoc("On the JVM, non-nullable values of this type are represented as values of the primitive type `${thisKind.name.lowercase()}`.")
    }
}