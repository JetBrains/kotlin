/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * This class provides annotations to [GranularAnnotationsBox].
 *
 * [EmptyAnnotationsProvider] is just an empty provider.
 * [SymbolAnnotationsProvider] is a provider based on [KaAnnotatedSymbol] API.
 * [CompositeAnnotationsProvider] is a composition of some [AnnotationsProvider].
 *
 * @see [GranularAnnotationsBox]
 */
internal sealed interface AnnotationsProvider {
    /**
     * @return a list of [AnnotationApplication] applicable for this provider
     */
    fun annotationInfos(): List<AnnotationApplication>
    operator fun get(classId: ClassId): List<AnnotationApplication>
    operator fun contains(classId: ClassId): Boolean

    /**
     * Example:
     * ```
     * package one
     *
     * @Ann1 @Ann2
     * class Foo
     * ```
     * If this provider provides annotations from `Foo` class then the result of the function will be [ClassId] for `one.Foo`
     *
     * @return [ClassId] of an owner of annotations from this provider
     */
    fun ownerClassId(): ClassId?
}
