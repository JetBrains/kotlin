/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType

open class ConeTypeRendererForDebugInfo protected constructor() : ConeTypeRenderer(ConeAttributeRenderer.ForReadability) {
    constructor(builder: StringBuilder) : this() {
        this.builder = builder
        idRenderer = ConeIdRendererForDiagnostics()
        idRenderer.builder = builder
    }

    override fun render(flexibleType: ConeFlexibleType) {
        builder.append("(")
        render(flexibleType.lowerBound)
        builder.append("..")
        render(flexibleType.upperBound)
        builder.append(")")
    }

    override fun render(type: ConeIntegerLiteralType) {
        render(type.getApproximatedType())
    }

    override fun ConeKotlinType.renderAttributes() {
        renderNonCompilerAttributes()
    }

    override fun render(type: ConeIntersectionType) {
        for ((index, intersected) in type.intersectedTypes.withIndex()) {
            if (index > 0) {
                builder.append(" & ")
            }
            this.render(intersected)
        }
    }
}
