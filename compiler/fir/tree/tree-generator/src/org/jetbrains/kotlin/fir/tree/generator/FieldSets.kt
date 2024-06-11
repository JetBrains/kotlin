/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.annotation
import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.block
import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.controlFlowGraphReference
import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.declaration
import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.declarationStatus
import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.expression
import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.reference
import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.typeParameter
import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.typeParameterRef
import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.typeProjection
import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.typeRef
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.generators.tree.AbstractField
import org.jetbrains.kotlin.generators.tree.ClassRef

object FieldSets {
    val typeArguments = fieldSet(listField("typeArguments", typeProjection, useMutableOrEmpty = true, withReplace = true))

    val declarations = fieldSet(listField(declaration).apply { useInBaseTransformerDetection = false })

    val annotations = fieldSet(
        listField(
            "annotations",
            annotation,
            withReplace = true,
            useMutableOrEmpty = true,
            withTransform = true,
        ).apply {
            needTransformInOtherChildren = true
        }
    )

    val typeParameters = fieldSet(listField("typeParameters", typeParameter))

    val name = fieldSet(field(nameType))

    fun declaredSymbol(name: String, symbolType: ClassRef<*>): Field =
        field(name, symbolType)
            .apply {
                symbolFieldRole = AbstractField.SymbolFieldRole.DECLARED
                skippedInCopy = true
            }

    fun declaredSymbol(symbolType: ClassRef<*>): Field = declaredSymbol("symbol", symbolType)

    fun referencedSymbol(name: String, symbolType: ClassRef<*>, nullable: Boolean = false, withReplace: Boolean = false): Field =
        field(name, symbolType, nullable, withReplace)
            .apply { symbolFieldRole = AbstractField.SymbolFieldRole.REFERENCED }

    fun referencedSymbol(symbolType: ClassRef<*>, nullable: Boolean = false, withReplace: Boolean = false): Field =
        referencedSymbol("symbol", symbolType, nullable, withReplace)
}
