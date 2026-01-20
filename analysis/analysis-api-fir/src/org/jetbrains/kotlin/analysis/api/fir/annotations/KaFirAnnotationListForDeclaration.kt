/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.utils.relfection.renderAsDataClassToString
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.name.ClassId

internal class KaFirAnnotationListForDeclaration private constructor(
    val firSymbol: FirBasedSymbol<*>,
    private val builder: KaSymbolByFirBuilder,
    private val stringBuilder: StringBuilder? = null,
) : AbstractList<KaAnnotation>(), KaAnnotationList {
    private val backingAnnotations by lazy {
        annotations(firSymbol, builder, stringBuilder = stringBuilder).also {
            stringBuilder?.append("Resolved lazy backing annotations in Fir")?.append(System.lineSeparator())
        }
    }

    override val token: KaLifetimeToken
        get() = builder.token

    private val useSiteSession: FirSession
        get() = builder.rootSession

    override fun isEmpty(): Boolean = withValidityAssertion {
        // isEmpty check needs to be performed on an analyzed declaration
        // (annotations can move to a nested declaration after code analysis).
        // See 'FirTypeResolveTransformer.moveOrDeleteIrrelevantAnnotations()'
        return backingAnnotations.isEmpty()
    }

    override val size: Int
        get() = withValidityAssertion { backingAnnotations.size }

    override fun iterator(): Iterator<KaAnnotation> = withValidityAssertion {
        stringBuilder?.append("Iterator called for annotation list in Fir")?.append(System.lineSeparator())
        return backingAnnotations.iterator()
    }

    override fun get(index: Int): KaAnnotation = withValidityAssertion {
        return backingAnnotations[index]
    }

    override fun contains(classId: ClassId): Boolean = withValidityAssertion {
        return hasAnnotation(firSymbol, classId, useSiteSession)
    }

    override fun get(classId: ClassId): List<KaAnnotation> = withValidityAssertion {
        return annotationsByClassId(firSymbol, classId, builder)
    }

    override val classIds: Collection<ClassId>
        get() = withValidityAssertion {
            stringBuilder?.append("Getting classIds in KaFirAnnotationListForDeclration")?.append(System.lineSeparator())
            annotationClassIds(firSymbol, useSiteSession, stringBuilder = stringBuilder)
        }

    companion object {
        fun create(firSymbol: FirBasedSymbol<*>, builder: KaSymbolByFirBuilder, stringBuilder: StringBuilder? = null): KaAnnotationList {
            return when {
                firSymbol is FirBackingFieldSymbol && firSymbol.propertySymbol.annotations.any { it.useSiteTarget == null } -> {
                    stringBuilder?.append("Creating annotations for backing field that has annotations without use-site targets ")?.append(
                        System.lineSeparator()
                    )
                    KaFirAnnotationListForDeclaration(firSymbol, builder, stringBuilder)
                }
                firSymbol.annotations.isEmpty() -> {
                    stringBuilder
                        ?.append(
                            "firSymbol annotations are empty, creating empty annotation list, " + System.lineSeparator() +
                                    "symbol is ${firSymbol::class.java.canonicalName}, " + System.lineSeparator() +
                                    "renderedSymbol is ${firSymbol.renderAsDataClassToString()} " + System.lineSeparator() +
                                    "its annotations is ${firSymbol.annotations::class.java}"
                        )?.append(System.lineSeparator())
                    KaBaseEmptyAnnotationList(builder.token)
                }
                else -> {
                    stringBuilder?.append(
                        "Creating annotations from else branch, " + System.lineSeparator() +
                                "symbol is ${firSymbol::class.java.canonicalName}, " + System.lineSeparator() +
                                "renderedSymbol is ${firSymbol.renderAsDataClassToString()} " + System.lineSeparator() +
                                "its annotations is ${firSymbol.annotations::class.java} with size ${firSymbol.annotations.size}"
                    )?.append(
                        System.lineSeparator()
                    )
                    KaFirAnnotationListForDeclaration(firSymbol, builder, stringBuilder)
                }
            }
        }
    }
}
