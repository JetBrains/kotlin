/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement
import java.util.Objects

class KaAnnotationImpl(
    classId: ClassId?,
    psi: KtCallElement?,
    useSiteTarget: AnnotationUseSiteTarget?,
    hasArguments: Boolean,

    /**
     * A list of annotation arguments which were applied when constructing annotation. Every argument is [KaAnnotationValue]
     */
    lazyArguments: Lazy<List<KaNamedAnnotationValue>>,
    index: Int?,

    /**
     * The constructor symbol into which this annotation resolves if the annotation is correctly resolved
     */
    constructorSymbol: KaConstructorSymbol?,
    override val token: KaLifetimeToken
) : KaAnnotation {
    private val backingClassId: ClassId? = classId

    override val classId: ClassId?
        get() = withValidityAssertion { backingClassId }

    private val backingPsi: KtCallElement? = psi

    override val psi: KtCallElement?
        get() = withValidityAssertion { backingPsi }

    private val backingUseSiteTarget: AnnotationUseSiteTarget? = useSiteTarget

    override val useSiteTarget: AnnotationUseSiteTarget?
        get() = withValidityAssertion { backingUseSiteTarget }

    private val backingHasArguments: Boolean = hasArguments

    override val hasArguments: Boolean
        get() = withValidityAssertion { backingHasArguments }

    private val backingArguments: List<KaNamedAnnotationValue> by lazyArguments

    override val arguments: List<KaNamedAnnotationValue>
        get() = withValidityAssertion { backingArguments }

    private val backingIndex: Int? = index

    override val index: Int?
        get() = withValidityAssertion { backingIndex }

    private val backingConstructorSymbol: KaConstructorSymbol? = constructorSymbol

    override val constructorSymbol: KaConstructorSymbol?
        get() = withValidityAssertion { backingConstructorSymbol }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KaAnnotationImpl &&
                backingClassId == other.backingClassId &&
                backingPsi == other.backingPsi &&
                backingUseSiteTarget == other.backingUseSiteTarget &&
                backingHasArguments == other.backingHasArguments &&
                backingIndex == other.backingIndex &&
                backingConstructorSymbol == other.backingConstructorSymbol &&
                backingArguments == other.backingArguments
    }

    override fun hashCode(): Int {
        return Objects.hash(
            backingClassId,
            backingPsi,
            backingUseSiteTarget,
            backingHasArguments,
            backingIndex,
            backingConstructorSymbol,
            backingArguments,
        )
    }

    override fun toString(): String {
        return "KaAnnotationApplicationWithArgumentsInfo(classId=" + backingClassId + ", psi=" + backingPsi + ", useSiteTarget=" +
                backingUseSiteTarget + ", hasArguments=" + backingHasArguments + ", index=" + backingIndex + ", constructorSymbol=" +
                backingConstructorSymbol + ", arguments=" + backingArguments + ")"
    }
}