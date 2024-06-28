/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.annotations

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtElement

@KaAnalysisApiInternals
class KaUnsupportedAnnotationValueImpl(
    override val token: KaLifetimeToken
) : KaAnnotationValue.UnsupportedValue {
    override val sourcePsi: KtElement?
        get() = withValidityAssertion { null }
}

@KaAnalysisApiInternals
class KaArrayAnnotationValueImpl(
    values: Collection<KaAnnotationValue>,
    sourcePsi: KtElement?,
    override val token: KaLifetimeToken,
) : KaAnnotationValue.ArrayValue {
    private val backingValues = values
    private val backingSourcePsi = sourcePsi

    override val values: Collection<KaAnnotationValue>
        get() = withValidityAssertion { backingValues }

    override val sourcePsi: KtElement?
        get() = withValidityAssertion { backingSourcePsi }
}

@KaAnalysisApiInternals
class KaNestedAnnotationAnnotationValueImpl(
    annotation: KaAnnotation,
    override val token: KaLifetimeToken,
) : KaAnnotationValue.NestedAnnotationValue {
    private val backingAnnotation = annotation

    override val annotation: KaAnnotation
        get() = withValidityAssertion { backingAnnotation }

    override val sourcePsi: KtElement?
        get() = withValidityAssertion { backingAnnotation.psi }
}

@KaAnalysisApiInternals
class KaClassLiteralAnnotationValueImpl(
    type: KaType,
    classId: ClassId?,
    sourcePsi: KtElement?,
    override val token: KaLifetimeToken,
) : KaAnnotationValue.ClassLiteralValue {
    private val backingType = type
    private val backingClassId = classId
    private val backingSourcePsi = sourcePsi

    override val type: KaType
        get() = withValidityAssertion { backingType }

    override val classId: ClassId?
        get() = withValidityAssertion { backingClassId }

    override val sourcePsi: KtElement?
        get() = withValidityAssertion { backingSourcePsi }
}

@KaAnalysisApiInternals
class KaEnumEntryAnnotationValueImpl(
    callableId: CallableId?,
    sourcePsi: KtElement?,
    override val token: KaLifetimeToken,
) : KaAnnotationValue.EnumEntryValue {
    private val backingCallableId = callableId
    private val backingSourcePsi = sourcePsi

    override val callableId: CallableId?
        get() = withValidityAssertion { backingCallableId }

    override val sourcePsi: KtElement?
        get() = withValidityAssertion { backingSourcePsi }
}

@KaAnalysisApiInternals
class KaConstantAnnotationValueImpl(
    value: KaConstantValue,
    override val token: KaLifetimeToken,
) : KaAnnotationValue.ConstantValue {
    private val backingValue = value

    override val value: KaConstantValue
        get() = withValidityAssertion { backingValue }

    override val sourcePsi: KtElement?
        get() = withValidityAssertion { backingValue.sourcePsi }
}
