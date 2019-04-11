/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.utils.DFS

fun IrClassifierSymbol.superTypes() = when (this) {
    is IrClassSymbol -> owner.superTypes
    is IrTypeParameterSymbol -> owner.superTypes
    else -> emptyList<IrType>()
}

fun IrClassifierSymbol.isSubtypeOfClass(superClass: IrClassSymbol): Boolean {
    if (FqNameEqualityChecker.areEqual(this, superClass)) return true
    return superTypes().any { it.isSubtypeOfClass(superClass) }
}

fun IrType.isSubtypeOfClass(superClass: IrClassSymbol): Boolean {
    if (this !is IrSimpleType) return false
    return classifier.isSubtypeOfClass(superClass)
}

fun Collection<IrClassifierSymbol>.commonSuperclass(): IrClassifierSymbol {
    var superClassifiers: MutableSet<IrClassifierSymbol>? = null

    require(isNotEmpty())

    val order = fold(emptyList<IrClassifierSymbol>()) { _, classifierSymbol ->
        val visited = mutableSetOf<IrClassifierSymbol>()
        DFS.topologicalOrder(
            listOf(classifierSymbol), { it.superTypes().map { s -> (s as IrSimpleType).classifier } },
            DFS.VisitedWithSet(visited)
        ).also {
            if (superClassifiers == null) {
                superClassifiers = visited
            } else {
                superClassifiers!!.apply {
                    retainAll { c -> visited.any { v -> FqNameEqualityChecker.areEqual(c, v) } }
                }
            }
        }
    }

    requireNotNull(superClassifiers)

    return order.firstOrNull { o -> superClassifiers!!.any { s -> FqNameEqualityChecker.areEqual(o, s) } }
        ?: error(
            "No common superType found for non-empty set of classifiers: ${joinToString(
                prefix = "[",
                postfix = "]"
            ) { it.owner.render() }}"
        )
}
