/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KaFe10DescMemberSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.callableIdIfNotLocal
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.load.java.descriptors.JavaPropertyDescriptor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal class KaFe10DescJavaFieldSymbol(
    override val descriptor: JavaPropertyDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaJavaFieldSymbol(), KaFe10DescMemberSymbol<JavaPropertyDescriptor> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val isStatic: Boolean
        get() = withValidityAssertion { DescriptorUtils.isStaticDeclaration(descriptor) }

    override val isVal: Boolean
        get() = withValidityAssertion { !descriptor.isVar }

    override val callableId: CallableId?
        get() = withValidityAssertion { descriptor.callableIdIfNotLocal }

    override val returnType: KaType
        get() = withValidityAssertion { descriptor.returnType.toKtType(analysisContext) }

    override fun createPointer(): KaSymbolPointer<KaJavaFieldSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaJavaFieldSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}