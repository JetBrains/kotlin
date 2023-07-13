/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCallCopy
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCopy
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import kotlin.reflect.KClass

/**
 * @param containerSymbols a list of symbols that should be resolved to make [annotations] are fully resolved.
 * Required only for "lazy" resolve mode in AA FIR to make a type annotation lazily resolved.
 * See KtFirAnnotationListForType for reference.
 * Example:
 * ```kotlin
 * fun foo(): @Anno Type
 * ```
 * This `Anno` annotation will have `foo` function as [containerSymbols].
 * More than one [containerSymbols] possible in case of type aliases:
 * ```kotlin
 * interface BaseInterface
 * typealias FirstTypeAlias = @Anno1 BaseInterface
 * typealias SecondTypeAlias = @Anno2 FirstTypeAlias
 *
 * fun foo(): @Anno3 SecondTypeAlias = TODO()
 * ```
 * here `@Anno3 SecondTypeAlias` will be expanded to ` @Anno1 @Anno2 @Anno3 BaseInterface`
 * and will have all intermediate type-aliases as [containerSymbols].
 */
class CustomAnnotationTypeAttribute(
    val annotations: List<FirAnnotation>,
    val containerSymbols: List<FirBasedSymbol<*>> = emptyList(),
) : ConeAttribute<CustomAnnotationTypeAttribute>() {
    constructor(annotations: List<FirAnnotation>, containerSymbol: FirBasedSymbol<*>?) : this(
        annotations,
        listOfNotNull(containerSymbol),
    )

    override fun union(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute? = null

    override fun intersect(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute? = null

    override fun add(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute {
        if (other == null || other === this) return this
        return CustomAnnotationTypeAttribute(annotations + other.annotations, containerSymbols + other.containerSymbols)
    }

    override fun isSubtypeOf(other: CustomAnnotationTypeAttribute?): Boolean = true

    override fun toString(): String = annotations.joinToString(separator = " ") { it.render() }

    override val key: KClass<out CustomAnnotationTypeAttribute>
        get() = CustomAnnotationTypeAttribute::class

    /**
     * Return an instance of the attribute that is not linked to any [containerSymbols].
     * It is required to avoid concurrent modification of those annotations from the linked
     * declaration and another call site (e.g., if a type was propagated to an anonymous function).
     *
     * See KT-60387 as an example of a possible concurrent problem.
     */
    fun independentInstance(): CustomAnnotationTypeAttribute = if (containerSymbols.isEmpty()) {
        this
    } else {
        CustomAnnotationTypeAttribute(
            annotations = annotations.map {
                if (it is FirAnnotationCall) {
                    buildAnnotationCallCopy(it) {}
                } else {
                    buildAnnotationCopy(it) {}
                }
            }
        )
    }
}

val ConeAttributes.custom: CustomAnnotationTypeAttribute? by ConeAttributes.attributeAccessor<CustomAnnotationTypeAttribute>()

val ConeAttributes.customAnnotations: List<FirAnnotation> get() = custom?.annotations.orEmpty()

val ConeKotlinType.customAnnotations: List<FirAnnotation> get() = attributes.customAnnotations
