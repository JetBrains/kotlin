/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KtFe10DescMemberSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.callableIdIfNotLocal
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal class KtFe10DescJavaFieldSymbol(
    override val descriptor: JavaPropertyDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtJavaFieldSymbol(), KtFe10DescMemberSymbol<JavaPropertyDescriptor> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val isStatic: Boolean
        get() = withValidityAssertion { DescriptorUtils.isStaticDeclaration(descriptor) }

    override val isVal: Boolean
        get() = withValidityAssertion { !descriptor.isVar }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { descriptor.callableIdIfNotLocal }

    override val returnType: KtType
        get() = withValidityAssertion { descriptor.returnType.toKtType(analysisContext) }

    override fun createPointer(): KtSymbolPointer<KtJavaFieldSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtJavaFieldSymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}