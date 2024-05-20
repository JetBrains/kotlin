/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement
import java.util.Objects

/**
 * @see KaAnnotated.annotations
 * @see KaAnnotationApplicationInfo
 */
public class KaAnnotationApplicationWithArgumentsInfo(
    classId: ClassId?,
    psi: KtCallElement?,
    useSiteTarget: AnnotationUseSiteTarget?,

    /**
     * A list of annotation arguments which were applied when constructing annotation. Every argument is [KaAnnotationValue]
     */
    arguments: List<KaNamedAnnotationValue>,
    index: Int?,

    /**
     * The constructor symbol into which this annotation resolves if the annotation is correctly resolved
     */
    constructorSymbolPointer: KaSymbolPointer<KaConstructorSymbol>?,
    public override val token: KaLifetimeToken
) : KaAnnotationApplication {
    private val backingClassId: ClassId? = classId

    public override val classId: ClassId?
        get() = withValidityAssertion { backingClassId }

    private val backingPsi: KtCallElement? = psi

    public override val psi: KtCallElement?
        get() = withValidityAssertion { backingPsi }

    private val backingUseSiteTarget: AnnotationUseSiteTarget? = useSiteTarget

    public override val useSiteTarget: AnnotationUseSiteTarget?
        get() = withValidityAssertion { backingUseSiteTarget }

    override val isCallWithArguments: Boolean
        get() = withValidityAssertion { backingArguments.isNotEmpty() }

    private val backingArguments: List<KaNamedAnnotationValue> = arguments

    public val arguments: List<KaNamedAnnotationValue>
        get() = withValidityAssertion { backingArguments }

    private val backingIndex: Int? = index

    override val index: Int?
        get() = withValidityAssertion { backingIndex }

    private val backingConstructorSymbolPointer: KaSymbolPointer<KaConstructorSymbol>? = constructorSymbolPointer

    public val constructorSymbolPointer: KaSymbolPointer<KaConstructorSymbol>?
        get() = withValidityAssertion { backingConstructorSymbolPointer }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KaAnnotationApplicationWithArgumentsInfo &&
                backingClassId == other.backingClassId &&
                backingPsi == other.backingPsi &&
                backingUseSiteTarget == other.backingUseSiteTarget &&
                backingArguments == other.backingArguments &&
                backingIndex == other.backingIndex &&
                backingConstructorSymbolPointer == other.backingConstructorSymbolPointer
    }

    override fun hashCode(): Int {
        return Objects.hash(
            backingClassId,
            backingPsi,
            backingUseSiteTarget,
            backingArguments,
            backingIndex,
            backingConstructorSymbolPointer
        )
    }

    override fun toString(): String {
        return "KaAnnotationApplicationWithArgumentsInfo(classId=" + backingClassId + ", psi=" + backingPsi + ", useSiteTarget=" +
                backingUseSiteTarget + ", arguments=" + backingArguments + ", index=" + backingIndex + ", constructorSymbolPointer=" +
                backingConstructorSymbolPointer + ")"
    }
}

public typealias KtAnnotationApplicationWithArgumentsInfo = KaAnnotationApplicationWithArgumentsInfo