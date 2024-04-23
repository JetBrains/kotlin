/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement
import java.util.*

/**
 * A lightweight implementation of [KtAnnotationApplication].
 * Should be used instead of [KtAnnotationApplicationWithArgumentsInfo] where possible to avoid redundant resolve.
 *
 * Example:
 * ```
 * @Anno1(1) @Anno2
 * class Foo
 * ```
 * In this case if you don't want to process [KtAnnotationApplicationWithArgumentsInfo.arguments]
 * you can call [KtAnnotated.annotationInfos] to get all necessary information.
 *
 * @see KtAnnotated.annotationInfos
 * @see KtAnnotationApplicationWithArgumentsInfo
 */
public class KtAnnotationApplicationInfo(
    classId: ClassId?,
    psi: KtCallElement?,
    useSiteTarget: AnnotationUseSiteTarget?,
    isCallWithArguments: Boolean,
    index: Int,
    public override val token: KtLifetimeToken
) : KtAnnotationApplication {
    private val backingClassId: ClassId? = classId

    public override val classId: ClassId?
        get() = withValidityAssertion { backingClassId }

    private val backingPsi: KtCallElement? = psi

    public override val psi: KtCallElement?
        get() = withValidityAssertion { backingPsi }

    private val backingUseSiteTarget: AnnotationUseSiteTarget? = useSiteTarget

    public override val useSiteTarget: AnnotationUseSiteTarget?
        get() = withValidityAssertion { backingUseSiteTarget }

    private val backingIsCallWithArguments: Boolean = isCallWithArguments

    public override val isCallWithArguments: Boolean
        get() = withValidityAssertion { backingIsCallWithArguments }

    private val backingIndex: Int = index

    public override val index: Int
        get() = withValidityAssertion { backingIndex }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KtAnnotationApplicationInfo &&
                backingClassId == other.backingClassId &&
                backingPsi == other.backingPsi &&
                backingUseSiteTarget == other.backingUseSiteTarget &&
                backingIsCallWithArguments == other.backingIsCallWithArguments &&
                backingIndex == other.backingIndex
    }

    override fun hashCode(): Int {
        return Objects.hash(backingClassId, backingPsi, backingUseSiteTarget, backingIsCallWithArguments, backingIndex)
    }

    override fun toString(): String {
        return "KtAnnotationApplicationInfo(classId=" + backingClassId + ", psi=" + backingPsi + ", useSiteTarget=" + backingUseSiteTarget +
                ", isCallWithArguments=" + backingIsCallWithArguments + ", index=" + backingIndex + ")"
    }
}
