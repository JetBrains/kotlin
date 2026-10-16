/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationTarget
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsSymbolInformationProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget

@KaImplementationDetail
abstract class KaBaseSymbolInformationProvider<T : KaSession> :
    KaBaseSessionComponent<T>(), KaInternalsSymbolInformationProvider {
    protected abstract fun computeAnnotationApplicableTargets(symbol: KaClassSymbol): Set<KotlinTarget>?

    override fun isDeprecated(symbol: KaSymbol): Boolean = withValidityAssertion { deprecation(symbol) != null }

    override fun isInline(symbol: KaKotlinPropertySymbol): Boolean = withValidityAssertion {
        symbol.getter?.isInline == true && (symbol.isVal || symbol.setter?.isInline == true)
    }

    override fun applicableAnnotationTargets(symbol: KaClassSymbol): Set<KaAnnotationTarget>? = withValidityAssertion {
        computeAnnotationApplicableTargets(symbol)?.mapNotNullTo(mutableSetOf()) { it.toKaAnnotationTarget() }
    }
}

@KaImplementationDetail
fun KotlinTarget.toKaAnnotationTarget(): KaAnnotationTarget? = when (this) {
    KotlinTarget.CLASS -> KaAnnotationTarget.CLASS
    KotlinTarget.ANNOTATION_CLASS -> KaAnnotationTarget.ANNOTATION_CLASS
    KotlinTarget.TYPE_PARAMETER -> KaAnnotationTarget.TYPE_PARAMETER
    KotlinTarget.PROPERTY -> KaAnnotationTarget.PROPERTY
    KotlinTarget.FIELD -> KaAnnotationTarget.FIELD
    KotlinTarget.LOCAL_VARIABLE -> KaAnnotationTarget.LOCAL_VARIABLE
    KotlinTarget.VALUE_PARAMETER -> KaAnnotationTarget.VALUE_PARAMETER
    KotlinTarget.CONSTRUCTOR -> KaAnnotationTarget.CONSTRUCTOR
    KotlinTarget.FUNCTION -> KaAnnotationTarget.FUNCTION
    KotlinTarget.PROPERTY_GETTER -> KaAnnotationTarget.PROPERTY_GETTER
    KotlinTarget.PROPERTY_SETTER -> KaAnnotationTarget.PROPERTY_SETTER
    KotlinTarget.TYPE -> KaAnnotationTarget.TYPE
    KotlinTarget.EXPRESSION -> KaAnnotationTarget.EXPRESSION
    KotlinTarget.FILE -> KaAnnotationTarget.FILE
    KotlinTarget.TYPEALIAS -> KaAnnotationTarget.TYPEALIAS
    else -> null
}
