/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.renderer.replacePrefixesInTypeRepresentations
import org.jetbrains.kotlin.renderer.typeStringsDifferOnlyInNullability
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

open class ConeTypeRendererForReadability(
    private val preRenderedConstructors: Map<TypeConstructorMarker, String>? = null,
    private val idRendererCreator: () -> ConeIdRenderer,
) : ConeTypeRendererForDebugInfo() {
    constructor(
        builder: StringBuilder,
        preRenderedConstructors: Map<TypeConstructorMarker, String>? = null,
        idRendererCreator: () -> ConeIdRenderer,
    ) : this(preRenderedConstructors, idRendererCreator) {
        this.builder = builder
        this.idRenderer = idRendererCreator()
        idRenderer.builder = builder
    }

    override fun render(flexibleType: ConeFlexibleType) {
        val lower = flexibleType.lowerBound

        val lowerRendered = renderBound(lower)
        val upperRendered = renderBound(flexibleType.upperBound)

        val rendered =
            renderFlexibleTypeCompact(lowerRendered, upperRendered)
                ?: run {
                    if (lower is ConeDefinitelyNotNullType) {
                        renderFlexibleTypeCompact(renderBound(lower.original), upperRendered)
                    } else null
                }
                ?: "($lowerRendered..$upperRendered)"

        builder.append(rendered)
    }

    private fun renderBound(bound: ConeKotlinType): String {
        val renderer = ConeTypeRendererForReadability(StringBuilder(), preRenderedConstructors, idRendererCreator)
        renderer.render(bound)
        return renderer.builder.toString()
    }

    private fun renderFlexibleTypeCompact(lowerRendered: String, upperRendered: String): String? {
        // More precise handling of different cases inside typeStringsDifferOnlyInNullability
        when {
            lowerRendered == upperRendered.replace("?", "") -> return upperRendered.replace("?", "!")
            upperRendered.endsWith("?") && ("$lowerRendered?") == upperRendered -> return "$lowerRendered!"
            "($lowerRendered)?" == upperRendered -> return "($lowerRendered)!"
        }

        val kotlinCollectionsPrefix = (StandardNames.COLLECTIONS_PACKAGE_FQ_NAME.asString() + ".").takeIf { lowerRendered.startsWith(it) } ?: ""
        val mutablePrefix = "Mutable"
        // java.util.List<Foo> -> (Mutable)List<Foo!>!
        val simpleCollection = replacePrefixesInTypeRepresentations(
            lowerRendered,
            kotlinCollectionsPrefix + mutablePrefix,
            upperRendered,
            kotlinCollectionsPrefix,
            "$kotlinCollectionsPrefix($mutablePrefix)"
        )
        if (simpleCollection != null) return simpleCollection
        // java.util.Map.Entry<Foo, Bar> -> (Mutable)Map.(Mutable)Entry<Foo!, Bar!>!
        val mutableEntry = replacePrefixesInTypeRepresentations(
            lowerRendered,
            kotlinCollectionsPrefix + "MutableMap.MutableEntry",
            upperRendered,
            kotlinCollectionsPrefix + "Map.Entry",
            "$kotlinCollectionsPrefix(Mutable)Map.(Mutable)Entry"
        )
        if (mutableEntry != null) return mutableEntry

        val kotlinPrefix = (StandardNames.BUILT_INS_PACKAGE_FQ_NAME.asString() + ".").takeIf { lowerRendered.startsWith(it) } ?: ""
        // Foo[] -> Array<(out) Foo!>!
        val array = replacePrefixesInTypeRepresentations(
            lowerRendered = lowerRendered,
            lowerPrefix = kotlinPrefix + "Array<",
            upperRendered = upperRendered,
            upperPrefix = kotlinPrefix + "Array<out ",
            foldedPrefix = kotlinPrefix + "Array<(out) "
        )
        if (array != null) return array
        return null
    }

    override fun renderConstructor(constructor: TypeConstructorMarker, nullabilityMarker: String) {
        preRenderedConstructors?.get(constructor)?.let {
            builder.append(it.replace("^", nullabilityMarker))
            return
        }
        super.renderConstructor(constructor, nullabilityMarker)
    }
}
