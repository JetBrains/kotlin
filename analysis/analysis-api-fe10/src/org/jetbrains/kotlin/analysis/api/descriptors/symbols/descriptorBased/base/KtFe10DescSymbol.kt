/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10AnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

internal interface KtFe10DescSymbol<T : DeclarationDescriptor> : KtFe10Symbol, KtFe10AnnotatedSymbol {
    val descriptor: T

    override val annotationsObject: Annotations
        get() = descriptor.annotations

    val source: SourceElement
        get() {
            val descriptor = this.descriptor
            if (descriptor is CallableMemberDescriptor && descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                val firstOverridden = descriptor.overriddenDescriptors.firstOrNull()
                if (firstOverridden != null) {
                    return firstOverridden.source
                }
            }

            return (descriptor as? DeclarationDescriptorWithSource)?.source ?: SourceElement.NO_SOURCE
        }

    override val psi: PsiElement?
        get() = withValidityAssertion { (source as? PsiSourceElement)?.psi }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion {
            val descriptor = this.descriptor
            if (descriptor is CallableMemberDescriptor) {
                // TODO: Add SUBSTITUTION_OVERRIDE when supported in HL API
                when (descriptor.kind) {
                    CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> return KtSymbolOrigin.INTERSECTION_OVERRIDE
                    CallableMemberDescriptor.Kind.SYNTHESIZED -> return KtSymbolOrigin.SOURCE_MEMBER_GENERATED
                    else -> {}
                }
            }

            when (source) {
                is KotlinSourceElement -> KtSymbolOrigin.SOURCE
                is JavaSourceElement -> KtSymbolOrigin.JAVA
                else -> KtSymbolOrigin.LIBRARY
            }
        }
}