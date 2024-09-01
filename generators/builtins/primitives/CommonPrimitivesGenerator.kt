/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType
import java.io.PrintWriter

class CommonPrimitivesGenerator(writer: PrintWriter) : BasePrimitivesGenerator(writer) {
    override fun FileBuilder.modifyGeneratedFile() {
        import("kotlin.internal.ActualizeByJvmBuiltinProvider")
    }

    override fun ClassBuilder.modifyGeneratedClass(thisKind: PrimitiveType) {
        annotations += "ActualizeByJvmBuiltinProvider"
        expectActual = ExpectActualModifier.Expect
    }

    override fun CompanionObjectBuilder.modifyGeneratedCompanionObject(thisKind: PrimitiveType) {
        if (thisKind.isFloatingPoint) {
            annotations += """Suppress("EXPECTED_PROPERTY_INITIALIZER", "DIVISION_BY_ZERO")"""
        } else {
            annotations += """Suppress("EXPECTED_PROPERTY_INITIALIZER")"""
        }
    }

    override fun MethodBuilder.modifyGeneratedRangeTo(thisKind: PrimitiveType, otherKind: PrimitiveType, opReturnType: PrimitiveType) {
        noBody()
    }

    override fun MethodBuilder.modifyGeneratedRangeUntil(thisKind: PrimitiveType, otherKind: PrimitiveType, opReturnType: PrimitiveType) {
        noBody()
    }

    override fun MethodBuilder.modifyGeneratedHashCode(thisKind: PrimitiveType) {
        noBody()
    }

    override fun ClassBuilder.generateAdditionalMethods(thisKind: PrimitiveType) {
        generateHashCode(thisKind)
    }
}