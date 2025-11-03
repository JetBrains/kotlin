/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.getConstructor
import org.jetbrains.kotlin.fir.types.lookupTagIfAny
import org.jetbrains.kotlin.renderer.renderFlexibleMutabilityOrArrayElementVarianceType
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import java.text.MessageFormat

open class ConeTypeRendererForReadability(
    private val preRenderedConstructors: Map<TypeConstructorMarker, String?>? = null,
    private val idRendererCreator: () -> ConeIdRenderer,
) : ConeTypeRendererForDebugInfo(coneAttributeRendererForReadability = ConeAttributeRenderer.None) {
    constructor(
        builder: StringBuilder,
        preRenderedConstructors: Map<TypeConstructorMarker, String?>? = null,
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
        return with(newRenderer()) {
            render(bound)
            builder.toString()
        }
    }

    private fun newRenderer(): ConeTypeRendererForReadability {
        return ConeTypeRendererForReadability(StringBuilder(), preRenderedConstructors, idRendererCreator)
    }

    private fun renderFlexibleTypeCompact(lowerRendered: String, upperRendered: String): String? {
        // More precise handling of different cases inside typeStringsDifferOnlyInNullability
        when {
            lowerRendered == upperRendered.replace("?", "") -> return upperRendered.replace("?", "!")
            upperRendered.endsWith("?") && ("$lowerRendered?") == upperRendered -> return "$lowerRendered!"
            "($lowerRendered)?" == upperRendered -> return "($lowerRendered)!"
        }

        return renderFlexibleMutabilityOrArrayElementVarianceType(
            lowerRendered,
            upperRendered,
            { (StandardNames.COLLECTIONS_PACKAGE_FQ_NAME.asString() + ".").takeIf { lowerRendered.startsWith(it) } ?: "" },
            { (StandardNames.BUILT_INS_PACKAGE_FQ_NAME.asString() + ".").takeIf { lowerRendered.startsWith(it) } ?: "" },
        )
    }

    override fun renderSimpleType(type: ConeSimpleKotlinType, nullabilityMarker: String) {
        val constructor = type.getConstructor()
        preRenderedConstructors?.get(constructor)?.let {
            val formatted = MessageFormat.format(it, *type.typeArguments.map { renderTypeArgument(it) }.toTypedArray(), nullabilityMarker)
            builder.append(formatted)
            return
        }

        super.renderSimpleType(type, nullabilityMarker)
    }

    private fun renderTypeArgument(typeArgument: ConeTypeProjection): String {
        return with(newRenderer()) {
            typeArgument.render()
            builder.toString()
        }
    }

    override fun couldBenefitFromParenthesizing(projection: ConeTypeProjection): Boolean {
        val constructor = (projection as? ConeKotlinType)?.lookupTagIfAny

        return when {
            constructor != null && preRenderedConstructors?.get(constructor)?.contains(" ") == true -> true
            else -> super.couldBenefitFromParenthesizing(projection)
        }
    }

    override fun renderDiagnostic(diagnostic: ConeDiagnostic, prefix: String, suffix: String): String {
        return "??? (${diagnostic.readableDescriptionAsTypeConstructor})"
    }
}
