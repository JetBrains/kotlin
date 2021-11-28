/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedConstantValue
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.maybeLocalClassId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtConstantValue
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationDescriptor
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

internal class KtFe10DescAnnotationApplication(
    private val descriptor: AnnotationDescriptor,
    override val token: ValidityToken
) : KtAnnotationApplication() {
    override val classId: ClassId?
        get() = withValidityAssertion { descriptor.annotationClass?.maybeLocalClassId }

    override val useSiteTarget: AnnotationUseSiteTarget?
        get() = withValidityAssertion {
            val psiTarget = (descriptor as? LazyAnnotationDescriptor)?.annotationEntry?.useSiteTarget ?: return null
            return psiTarget.getAnnotationUseSiteTarget()
        }

    val source: SourceElement
        get() = withValidityAssertion { descriptor.source }

    override val psi: KtCallElement?
        get() = withValidityAssertion { (source as? PsiSourceElement)?.psi as? KtAnnotationEntry }

    override val arguments: List<KtNamedConstantValue>
        get() = withValidityAssertion {
            descriptor.allValueArguments.map { (name, value) -> KtNamedConstantValue(name.asString(), value.toKtConstantValue()) }
        }
}