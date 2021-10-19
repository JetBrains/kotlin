/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiDefaultSetterParameterSymbol(
    private val accessorPsi: KtPropertyAccessor,
    override val analysisSession: KtFe10AnalysisSession
) : KtValueParameterSymbol(), KtFe10Symbol {
    private val descriptor: VariableDescriptor? by cached {
        val bindingContext = analysisSession.analyze(accessorPsi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.PROPERTY_ACCESSOR, accessorPsi]?.valueParameters?.single()
    }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { KtSymbolOrigin.SOURCE_MEMBER_GENERATED }

    override val hasDefaultValue: Boolean
        get() = withValidityAssertion { false }

    override val isVararg: Boolean
        get() = withValidityAssertion { false }

    override val annotatedType: KtTypeAndAnnotations
        get() = withValidityAssertion { descriptor?.type?.toKtTypeAndAnnotations(analysisSession) ?: createErrorTypeAndAnnotations() }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val name: Name
        get() = withValidityAssertion { descriptor?.name ?: Name.identifier("value") }

    override val annotations: List<KtAnnotationCall>
        get() = withValidityAssertion { emptyList() }

    override fun containsAnnotation(classId: ClassId): Boolean = withValidityAssertion {
        return false
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion { emptyList() }

    override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol> = withValidityAssertion {
        return KtFe10NeverRestoringSymbolPointer()
    }
}