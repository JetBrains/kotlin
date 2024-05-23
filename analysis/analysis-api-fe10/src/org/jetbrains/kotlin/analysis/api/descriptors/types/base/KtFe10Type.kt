/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types.base

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.annotations.KaFe10AnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KaFe10DebugTypeRenderer
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType

internal interface KaFe10Type : KaLifetimeOwner, KaAnnotated {
    val fe10Type: UnwrappedType

    val analysisContext: Fe10AnalysisContext

    override val annotationsList: KaAnnotationsList
        get() = withValidityAssertion {
            KaFe10AnnotationsList.create(
                fe10Type.annotations,
                analysisContext,
                ignoreAnnotations = setOf(
                    StandardClassIds.Annotations.ExtensionFunctionType,
                    StandardClassIds.Annotations.ContextFunctionTypeParams,
                )
            )
        }

    override val token: KaLifetimeToken
        get() = analysisContext.token
}

internal fun KotlinType.renderForDebugging(analysisContext: Fe10AnalysisContext): String {
    val renderer = KaFe10DebugTypeRenderer()
    return prettyPrint { renderer.render(analysisContext, this@renderForDebugging, this@prettyPrint) }
}