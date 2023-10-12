/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.callableIdIfNonLocal
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntryInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiEnumEntrySymbol(
    override val psi: KtEnumEntry,
    override val analysisContext: Fe10AnalysisContext
) : KtEnumEntrySymbol(), KtEnumEntryInitializerSymbol, KtFe10PsiSymbol<KtEnumEntry, ClassDescriptor> {
    override val descriptor: ClassDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.CLASS, psi]
    }

    override val containingEnumClassIdIfNonLocal: ClassId?
        get() = withValidityAssertion {
            val containingClass = psi.containingClass()?.takeIf { it.isEnum() } ?: return null
            return containingClass.getClassId()
        }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { psi.callableIdIfNonLocal }

    override val returnType: KtType
        get() = withValidityAssertion {
            val containingDescriptor = descriptor?.containingDeclaration
            if (containingDescriptor is ClassDescriptor && containingDescriptor.kind == ClassKind.ENUM_CLASS) {
                return containingDescriptor.defaultType.toKtType(analysisContext)
            } else {
                createErrorType()
            }
        }

    override val name: Name
        get() = withValidityAssertion { psi.nameAsSafeName }

    override val enumEntryInitializer: KtEnumEntryInitializerSymbol?
        get() = this.takeIf { psi.body != null }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtEnumEntrySymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtEnumEntrySymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }


    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
