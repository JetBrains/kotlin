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
        expectActual = ExpectActualModifier.Actual
    }

    override val fileAnnotations = listOf("kotlin.internal.ProducesBuiltinMetadata")

    override fun PropertyBuilder.modifyGeneratedCompanionObjectProperty(thisKind: PrimitiveType) {
        if (this.name in setOf("POSITIVE_INFINITY", "NEGATIVE_INFINITY", "NaN")) {
            suppressDiagnostics("DIVISION_BY_ZERO")
        }
    }

    override fun MethodBuilder.modifyGeneratedRangeTo(thisKind: PrimitiveType, otherKind: PrimitiveType, opReturnType: PrimitiveType) {
        noBody()
        suppressNonAbstractFunctionWithoutBody()
    }

    override fun MethodBuilder.modifyGeneratedRangeUntil(thisKind: PrimitiveType, otherKind: PrimitiveType, opReturnType: PrimitiveType) {
        noBody()
        suppressNonAbstractFunctionWithoutBody()
    }

    override fun MethodBuilder.modifyGeneratedCompareTo(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        suppressNonAbstractFunctionWithoutBody()
    }

    override fun MethodBuilder.modifyGeneratedBinaryOperation(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        suppressNonAbstractFunctionWithoutBody()
    }

    override fun MethodBuilder.modifyGeneratedUnaryOperation(thisKind: PrimitiveType) {
        suppressNonAbstractFunctionWithoutBody()
    }

    override fun MethodBuilder.modifyGeneratedConversions(thisKind: PrimitiveType, otherKind: PrimitiveType) {
        if (thisKind == PrimitiveType.INT && otherKind == PrimitiveType.CHAR) {
            annotations.clear()
            annotations += intrinsicConstEvaluationAnnotation
            suppressDiagnostics("OVERRIDE_DEPRECATION", "NON_ABSTRACT_FUNCTION_WITH_NO_BODY")
        } else {
            suppressNonAbstractFunctionWithoutBody()
        }
    }

    override fun MethodBuilder.modifyGeneratedBitShiftOperators(thisKind: PrimitiveType) {
        suppressNonAbstractFunctionWithoutBody()
    }

    override fun MethodBuilder.modifyGeneratedBitwiseOperators(thisKind: PrimitiveType) {
        suppressNonAbstractFunctionWithoutBody()
    }

    override fun MethodBuilder.modifyGeneratedEquals(thisKind: PrimitiveType) {
        suppressNonAbstractFunctionWithoutBody()
    }

    override fun MethodBuilder.modifyGeneratedToString(thisKind: PrimitiveType) {
        suppressNonAbstractFunctionWithoutBody()
    }

    override fun MethodBuilder.modifyGeneratedHashCode(thisKind: PrimitiveType) {
        suppressNonAbstractFunctionWithoutBody()
    }
}