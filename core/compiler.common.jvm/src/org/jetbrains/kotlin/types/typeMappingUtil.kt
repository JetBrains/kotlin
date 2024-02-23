/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("TypeMappingUtil")

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

fun TypeMappingMode.updateArgumentModeFromAnnotations(
    type: KotlinTypeMarker,
    typeSystem: TypeSystemCommonBackendContext,
    suppressWildcardsByContainingDeclaration: Boolean? = null,
): TypeMappingMode {
    type.suppressWildcardsMode(typeSystem)?.let {
        return TypeMappingMode.createWithConstantDeclarationSiteWildcardsMode(
            skipDeclarationSiteWildcards = it,
            isForAnnotationParameter = isForAnnotationParameter,
            needInlineClassWrapping = needInlineClassWrapping,
            mapTypeAliases = mapTypeAliases
        )
    }

    if (with(typeSystem) { type.hasAnnotation(JvmStandardClassIds.JVM_WILDCARD_ANNOTATION_FQ_NAME) }) {
        return TypeMappingMode.createWithConstantDeclarationSiteWildcardsMode(
            skipDeclarationSiteWildcards = false,
            isForAnnotationParameter = isForAnnotationParameter,
            fallbackMode = this,
            needInlineClassWrapping = needInlineClassWrapping,
            mapTypeAliases = mapTypeAliases
        )
    }

    // For example,
    //   @JvmSuppressWildcards(true)
    //   fun deepOpen(x: Out<Out<Out<Open>>>) {}
    // Instead of the return type, the annotation associated with the declaration indicates that its type signature,
    // including return type and parameter types, need to suppress wildcards.
    suppressWildcardsByContainingDeclaration?.let {
        return TypeMappingMode.createWithConstantDeclarationSiteWildcardsMode(
            skipDeclarationSiteWildcards = it,
            isForAnnotationParameter = isForAnnotationParameter,
            needInlineClassWrapping = needInlineClassWrapping,
            mapTypeAliases = mapTypeAliases
        )
    }

    return this
}

fun KotlinTypeMarker.suppressWildcardsMode(typeSystem: TypeSystemCommonBackendContext): Boolean? =
    with(typeSystem) {
        if (hasAnnotation(JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME))
            getAnnotationFirstArgumentValue(JVM_SUPPRESS_WILDCARDS_ANNOTATION_FQ_NAME) as? Boolean ?: true
        else null
    }
