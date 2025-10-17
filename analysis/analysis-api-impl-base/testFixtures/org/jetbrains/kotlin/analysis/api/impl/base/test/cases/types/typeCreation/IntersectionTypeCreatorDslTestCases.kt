/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId

@Suppress("UNUSED")
class IntersectionTypeCreatorDslTestCases(session: KaSession, caretToType: Map<String, KaType>) :
    AbstractTypeCreatorDslTest.TestCases(session, caretToType) {
    fun testSingleConjunct(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.intersectionType {
            conjunct { type }
        }
    }

    fun testThreeRandomTypes(): KaType {
        val type1 = getTypeByCaret("1")
        val type2 = getTypeByCaret("2")
        val type3 = getTypeByCaret("3")
        return session.typeCreator.intersectionType {
            conjunct { type1 }
            conjunct(type2)
            conjunct { type3 }
        }
    }

    fun testThreeSubtypes(): KaType {
        val type1 = getTypeByCaret("1")
        val type2 = getTypeByCaret("2")
        val type3 = getTypeByCaret("3")
        return session.typeCreator.intersectionType {
            conjunct { type1 }
            conjuncts {
                listOf(type2, type3)
            }
        }
    }

    fun testThreeUserSubtypes(): KaType {
        val type1 = getTypeByCaret("1")
        val type2 = getTypeByCaret("2")
        val type3 = getTypeByCaret("3")
        return session.typeCreator.intersectionType {
            conjunct { type1 }
            conjuncts(listOf(type2, type3))
        }
    }

    fun testWithError(): KaType {
        val type1 = getTypeByCaret("1")
        val type2 = getTypeByCaret("2")
        val type3 = getTypeByCaret("3")
        return session.typeCreator.intersectionType {
            conjuncts(listOf(type1, type2, type3))
        }
    }

    fun testDuplicates(): KaType {
        val type = getTypeByCaret("type")
        return session.typeCreator.intersectionType {
            conjuncts(listOf(type, type, type))
        }
    }

    fun testAnotherIntersectionTypeAsConjunct(): KaType {
        val type1 = getTypeByCaret("1")
        val type2 = getTypeByCaret("2")
        val type3 = getTypeByCaret("3")
        val intersection = session.typeCreator.intersectionType {
            conjunct { type1 }
            conjunct { type2 }
        }

        return session.typeCreator.intersectionType {
            conjunct(intersection)
            conjunct { type3 }
        }
    }

    fun testWithAnnotationsOnConjuncts(): KaType {
        val annotationClassId1 = ClassId.fromString("MyAnno1")
        val annotationClassId2 = ClassId.fromString("MyAnno2")
        val annotationClassId3 = ClassId.fromString("MyAnno3")

        val symbol1 = getClassLikeSymbolByCaret("1")
        val symbol2 = getClassLikeSymbolByCaret("2")

        return session.typeCreator.intersectionType {
            conjunct {
                classType(symbol1) {
                    annotations(listOf(annotationClassId1, annotationClassId2))
                }
            }

            conjunct {
                classType(symbol2) {
                    annotations(listOf(annotationClassId2, annotationClassId3))
                }
            }
        }
    }

    fun testFlexibleTypeAsConjunct(): KaType {
        val lowerBound = getTypeByCaret("lower")
        val upperBound = getTypeByCaret("upper")
        val type = getTypeByCaret("type")

        return session.typeCreator.intersectionType {
            flexibleType {
                this.lowerBound = lowerBound
                this.upperBound = upperBound
            }?.let {
                conjunct(it)
            }
            conjunct(type)
        }
    }
}