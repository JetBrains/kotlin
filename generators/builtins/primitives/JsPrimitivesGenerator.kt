/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import java.io.PrintWriter

class JsPrimitivesGenerator(writer: PrintWriter) : BasePrimitivesGenerator(writer) {
    override fun PrimitiveType.shouldGenerate(): Boolean {
        return this != PrimitiveType.LONG
    }

    override fun FileDescription.modifyGeneratedFile() {
        addSuppress("NON_ABSTRACT_FUNCTION_WITH_NO_BODY")
        addSuppress("UNUSED_PARAMETER")
    }

    override fun PropertyDescription.modifyGeneratedCompanionObjectProperty(thisKind: PrimitiveType) {
        if (this.name in setOf("POSITIVE_INFINITY", "NEGATIVE_INFINITY", "NaN")) {
            this.addAnnotation("Suppress(\"DIVISION_BY_ZERO\")")
        }
    }

    override fun generateAdditionalMethods(thisKind: PrimitiveType): List<MethodDescription> {
        val hashCode = MethodDescription(
            doc = null,
            signature = MethodSignature(
                isOverride = true,
                name = "hashCode",
                arg = null,
                returnType = PrimitiveType.INT.capitalized
            )
        )
        return listOf(hashCode)
    }
}