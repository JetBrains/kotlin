/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

class KtFe10DescDefaultPropertyGetterSymbol(
    private val propertyDescriptor: PropertyDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtPropertyGetterSymbol(), KtFe10Symbol {
    override val isDefault: Boolean
        get() = withValidityAssertion { true }

    override val isInline: Boolean
        get() = withValidityAssertion { false }

    override val isOverride: Boolean
        get() = withValidityAssertion { propertyDescriptor.isExplicitOverride }

    override val hasBody: Boolean
        get() = withValidityAssertion { false }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { propertyDescriptor.getterCallableIdIfNotLocal }

    override val returnType: KtType
        get() = withValidityAssertion { propertyDescriptor.type.toKtType(analysisContext) }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { propertyDescriptor.getSymbolOrigin(analysisContext) }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val receiverType: KtType?
        get() = withValidityAssertion { propertyDescriptor.extensionReceiverParameter?.type?.toKtType(analysisContext) }

    override val modality: Modality
        get() = withValidityAssertion { propertyDescriptor.ktModality }

    override val visibility: Visibility
        get() = withValidityAssertion { propertyDescriptor.ktVisibility }

    override val annotationsList: KtAnnotationsList
        get() = withValidityAssertion { KtEmptyAnnotationsList(token) }

    override fun createPointer(): KtSymbolPointer<KtPropertyGetterSymbol> {
        withValidityAssertion {
            return KtFe10NeverRestoringSymbolPointer()
        }
    }
}