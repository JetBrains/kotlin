/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.comparators

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.FirTypeRefComparator

object FirMemberDeclarationComparator : Comparator<FirMemberDeclaration> {
    // Comparing different kinds of callable members by assigning distinct priorities to those members.
    object TypeAndNameComparator : Comparator<FirMemberDeclaration> {
        private val FirMemberDeclaration.priority: Int
            get() = when (this) {
                is FirEnumEntry -> 9
                is FirConstructor -> 8
                is FirProperty -> receiverParameter?.let { 6 } ?: 7
                is FirField -> 5
                is FirFunction -> receiverParameter?.let { 3 } ?: 4
                is FirClass -> 2
                is FirTypeAlias -> 1
                is FirErrorProperty -> 0
                is FirValueParameter -> 0
                is FirBackingField -> 0
            }

        fun compareInternal(a: FirMemberDeclaration, b: FirMemberDeclaration): Int? {
            // Declarations with higher priority must go first.
            ifNotEqual(b.priority, a.priority) { return it }
            if (a is FirEnumEntry && b is FirEnumEntry) {
                // Never reorder enum entries.
                return 0
            }
            ifNotEqual(a.nameOrSpecialName, b.nameOrSpecialName) { return it }

            // Might be equal
            return null
        }

        override fun compare(a: FirMemberDeclaration, b: FirMemberDeclaration): Int = compareInternal(a, b) ?: 0
    }

    override fun compare(a: FirMemberDeclaration, b: FirMemberDeclaration): Int {
        when {
            a is FirCallableDeclaration && b is FirCallableDeclaration -> {
                return FirCallableDeclarationComparator.compare(a, b)
            }
            a is FirTypeAlias && b is FirTypeAlias -> {
                TypeAndNameComparator.compareInternal(a, b)?.let { return it }
                FirTypeRefComparator.compare(a.expandedTypeRef, b.expandedTypeRef).let {
                    if (it != 0) return it
                }
            }
            a is FirClass && b is FirClass -> {
                TypeAndNameComparator.compareInternal(a, b)?.let { return it }
                ifNotEqual(a.classKind.ordinal, b.classKind.ordinal) { return it }
                ifNotEqual(a.status.isCompanion, b.status.isCompanion) { return it }
            }
            else -> {
                error("Unsupported member declaration comparison: ${a.render()} v.s. ${b.render()}")
            }
        }
        ifRendersNotEqual(a, b) { return it }
        return a.moduleData.name.compareTo(b.moduleData.name)
    }
}
