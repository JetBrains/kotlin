/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement

/**
 * @see KtAnnotated.annotations
 * @see KtAnnotationApplicationInfo
 */
public data class KtAnnotationApplicationWithArgumentsInfo(
    override val classId: ClassId?,
    override val psi: KtCallElement?,
    override val useSiteTarget: AnnotationUseSiteTarget?,

    /**
     * A list of annotation arguments which were applied when constructing annotation. Every argument is [KtAnnotationValue]
     */
    public val arguments: List<KtNamedAnnotationValue>,
    override val index: Int?,

    /**
     * The constructor symbol into which this annotation resolves if the annotation is correctly resolved
     */
    public val constructorSymbolPointer: KtSymbolPointer<KtConstructorSymbol>?,
) : KtAnnotationApplication {
    override val isCallWithArguments: Boolean get() = arguments.isNotEmpty()
}
