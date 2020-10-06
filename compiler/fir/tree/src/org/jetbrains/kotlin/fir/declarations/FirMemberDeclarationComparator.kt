/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.FirTypeRefComparator
import org.jetbrains.kotlin.name.Name

object FirMemberDeclarationComparator : Comparator<FirMemberDeclaration> {
    // Comparing different kinds of callable members by assigning distinct priorities to those members.
    object TypeAndNameComparator : Comparator<FirMemberDeclaration> {
        private val FirMemberDeclaration.priority : Int
            get() = when (this) {
                is FirEnumEntry -> 7
                is FirConstructor -> 6
                is FirProperty -> 5
                is FirField -> 4
                is FirFunction<*> -> 3
                is FirClass<*> -> 2
                is FirTypeAlias -> 1
                else -> 0
            }

        private val FirMemberDeclaration.name : Name
            get() = when (this) {
                is FirCallableMemberDeclaration<*> ->
                    this.symbol.callableId.callableName
                is FirClass<*> ->
                    this.classId.shortClassName
                is FirTypeAlias ->
                    this.name
                else ->
                    error("Unsupported name retrieval for ${render()}")
            }

        override fun compare(a: FirMemberDeclaration, b: FirMemberDeclaration): Int {
            val priorityDiff = a.priority - b.priority
            if (priorityDiff != 0) {
                return priorityDiff
            }
            // Never reorder enum entries.
            if (a is FirEnumEntry) {
                require(b is FirEnumEntry) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                return 0
            }

            return a.name.compareTo(b.name)
        }
    }

    override fun compare(a: FirMemberDeclaration, b: FirMemberDeclaration): Int {
        if (a is FirCallableMemberDeclaration<*> && b is FirCallableMemberDeclaration<*>) {
            return FirCallableMemberDeclarationComparator.compare(a, b)
        }

        val typeAndNameDiff = TypeAndNameComparator.compare(a, b)
        if (typeAndNameDiff != 0) {
            return typeAndNameDiff
        }

        // Note that names are already compared. Check other details per kind.
        when (a) {
            is FirClass<*> -> {
                require(b is FirClass<*>) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                return a.classId.packageFqName.asString().compareTo(b.classId.packageFqName.asString())
            }
            is FirTypeAlias -> {
                require(b is FirTypeAlias) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                return FirTypeRefComparator.compare(a.expandedTypeRef, b.expandedTypeRef)
            }
            else ->
                error("Unsupported member declaration comparison: ${a.render()} v.s. ${b.render()}")
        }
    }
}
