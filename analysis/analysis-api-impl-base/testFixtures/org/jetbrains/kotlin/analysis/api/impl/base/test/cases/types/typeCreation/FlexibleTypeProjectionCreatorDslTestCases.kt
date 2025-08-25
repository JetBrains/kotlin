/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId

@Suppress("UNUSED")
class FlexibleTypeProjectionCreatorDslTestCases(session: KaSession, caretToType: Map<String, KaType>) :
    AbstractTypeCreatorDslTest.TestCases(session, caretToType) {
    fun testAnyAndNullableAny(): KaType? {
        val lowerBound = getTypeByCaret("lower")
        val upperBound = getTypeByCaret("upper")
        return session.typeCreator.flexibleType {
            this.lowerBound = lowerBound
            this.upperBound = upperBound
        }
    }

    fun testIntAndNullableInt(): KaType? {
        val lowerBound = getTypeByCaret("lower")
        val upperBound = getTypeByCaret("upper")
        return session.typeCreator.flexibleType {
            this.lowerBound = lowerBound
            this.upperBound = upperBound
        }
    }

    fun testNothingAndNullableAny(): KaType? {
        val lowerBound = getTypeByCaret("lower")
        val upperBound = getTypeByCaret("upper")
        return session.typeCreator.flexibleType {
            this.lowerBound = lowerBound
            this.upperBound = upperBound
        }
    }

    fun testTwoUserTypes(): KaType? {
        val lowerBound = getTypeByCaret("lower")
        val upperBound = getTypeByCaret("upper")
        return session.typeCreator.flexibleType {
            this.lowerBound = lowerBound
            this.upperBound = upperBound
        }
    }

    fun testTwoFlexibleTypes(): KaType? {
        val lowerBound1 = getTypeByCaret("lower1")
        val upperBound1 = getTypeByCaret("upper1")
        val lowerBound2 = getTypeByCaret("lower2")
        val upperBound2 = getTypeByCaret("upper2")
        val lowerFlexibleType = session.typeCreator.flexibleType {
            this.lowerBound = lowerBound1
            this.upperBound = upperBound1
        } ?: return null
        val upperFlexibleType = session.typeCreator.flexibleType {
            this.lowerBound = lowerBound2
            this.upperBound = upperBound2
        } ?: return null

        return session.typeCreator.flexibleType {
            this.lowerBound = lowerFlexibleType
            this.upperBound = upperFlexibleType
        }
    }

    fun testFlexibleTypeWithReplacedUpperBound(): KaType? {
        val flexibleType = getTypeByCaret("type") as KaFlexibleType
        val upperbound = getTypeByCaret("upper")
        return session.typeCreator.flexibleType(flexibleType) {
            upperBound = upperbound
        }
    }

    fun testWithAnnotations(): KaType? {
        val annotationClassId1 = ClassId.fromString("MyAnno1")
        val annotationClassId2 = ClassId.fromString("MyAnno2")
        val annotationClassId3 = ClassId.fromString("MyAnno3")

        val lowerBound = getTypeByCaret("lower")
        val upperBound = getTypeByCaret("upper")
        return session.typeCreator.flexibleType {
            this.lowerBound = lowerBound
            this.upperBound = upperBound
            annotations(listOf(annotationClassId1, annotationClassId2, annotationClassId3))
        }
    }

    fun testIncompatibleBounds(): KaType? {
        val lowerBound = getTypeByCaret("lower")
        val upperBound = getTypeByCaret("upper")
        return session.typeCreator.flexibleType {
            this.lowerBound = lowerBound
            this.upperBound = upperBound
        }
    }

    fun testEqualBounds(): KaType? {
        val bound = getTypeByCaret("type")
        return session.typeCreator.flexibleType {
            this.lowerBound = bound
            this.upperBound = bound
        }
    }

    fun testWithDefaultValues(): KaType? {
        return session.typeCreator.flexibleType()
    }

    fun testWithOnlyUpperBoundProvided(): KaType? {
        val type = getTypeByCaret("type")
        return session.typeCreator.flexibleType {
            upperBound = type
        }
    }

    fun testWithOnlyLowerBoundProvided(): KaType? {
        val type = getTypeByCaret("type")
        return session.typeCreator.flexibleType {
            lowerBound = type
        }
    }
}