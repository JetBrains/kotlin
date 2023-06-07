/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10AnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.references.fe10.base.KtFe10ReferenceResolutionHelper
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

internal interface KtFe10DescSymbol<T : DeclarationDescriptor> : KtFe10Symbol, KtFe10AnnotatedSymbol {
    val descriptor: T

    override val annotationsObject: Annotations
        get() = withValidityAssertion { descriptor.annotations }

    val source: SourceElement
        get() = withValidityAssertion {
            (descriptor as? DeclarationDescriptorWithSource)?.source ?: SourceElement.NO_SOURCE
        }

    override val psi: PsiElement?
        get() = withValidityAssertion {
            (source as? PsiSourceElement)?.psi
                ?: KtFe10ReferenceResolutionHelper.getInstance()
                    ?.findDecompiledDeclaration(analysisContext.resolveSession.project, descriptor, null)
        }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { descriptor.getSymbolOrigin(analysisContext) }
}