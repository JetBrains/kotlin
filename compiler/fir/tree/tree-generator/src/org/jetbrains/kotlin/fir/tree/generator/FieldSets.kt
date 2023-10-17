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
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFieldConfigurator
import org.jetbrains.kotlin.fir.tree.generator.context.type
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.generators.tree.TypeRefWithVariance
import org.jetbrains.kotlin.generators.tree.withArgs
import org.jetbrains.kotlin.types.Variance

object FieldSets {
    val calleeReference by lazy { field("calleeReference", reference, withReplace = true) }

    val receivers by lazy {
        fieldSet(
            field("explicitReceiver", expression, nullable = true, withReplace = true).withTransform(),
            field("dispatchReceiver", expression, nullable = true, withReplace = true),
            field("extensionReceiver", expression, nullable = true, withReplace = true)
        )
    }

    val typeArguments by lazy { fieldList("typeArguments", typeProjection, useMutableOrEmpty = true, withReplace = true) }

    val arguments by lazy { fieldList("arguments", expression) }

    val declarations by lazy { fieldList(declaration).apply { useInBaseTransformerDetection = false } }

    val annotations by lazy {
        fieldList(
            "annotations",
            annotation,
            withReplace = true,
            useMutableOrEmpty = true
        ).withTransform(needTransformInOtherChildren = true)
    }

    fun AbstractFieldConfigurator<*>.ConfigureContext.symbolWithPackageWithArgument(packageName: String, symbolClassName: String) =
        field("symbol", type(packageName, symbolClassName).withArgs(TypeRefWithVariance(Variance.OUT_VARIANCE, element)))

    fun symbolWithPackage(packageName: String, symbolClassName: String): Field =
        field("symbol", type(packageName, symbolClassName))

    fun AbstractFieldConfigurator<*>.ConfigureContext.symbolWithArgument(symbolClassName: String): Field =
        symbolWithPackageWithArgument("fir.symbols.impl", symbolClassName)

    fun symbol(symbolClassName: String): Field =
        symbolWithPackage("fir.symbols.impl", symbolClassName)

    fun body(nullable: Boolean = false, withReplace: Boolean = false) =
        field("body", block, nullable, withReplace = withReplace)

    val returnTypeRef =field("returnTypeRef", typeRef)

    val typeRefField = field(typeRef, withReplace = true)

    val typeParameters by lazy { fieldList("typeParameters", typeParameter) }

    val typeParameterRefs by lazy { fieldList("typeParameters", typeParameterRef) }

    val name by lazy { field(nameType) }

    val initializer by lazy { field("initializer", expression, nullable = true) }

    fun superTypeRefs(withReplace: Boolean = false) = fieldList("superTypeRefs", typeRef, withReplace)

    val classKind by lazy { field(classKindType) }

    val status by lazy { field("status", declarationStatus, withReplace = true) }

    val controlFlowGraphReferenceField by lazy { field("controlFlowGraphReference", controlFlowGraphReference, withReplace = true, nullable = true) }

    val visibility by lazy { field(visibilityType) }

    val effectiveVisibility by lazy { field("effectiveVisibility", effectiveVisibilityType) }

    fun modality(nullable: Boolean): Field {
        return field(modalityType, nullable = nullable)
    }

    val scopeProvider by lazy { field("scopeProvider", firScopeProviderType) }

    val smartcastStability by lazy { field(smartcastStabilityType) }
}
