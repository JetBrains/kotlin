/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
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

fun IrType.isSubtypeOf(superType: IrType, irBuiltIns: IrBuiltIns): Boolean {
    return AbstractTypeChecker.isSubtypeOf(IrTypeCheckerContext(irBuiltIns) as AbstractTypeCheckerContext, this, superType)
}

fun Collection<IrType>.commonSupertype(irBuiltIns: IrBuiltIns): IrType {
    return NewCommonSuperTypeCalculator.run {
        IrTypeCheckerContext(irBuiltIns).commonSuperType(map { it }) as IrType
    }
}

fun IrType.isNullable(): Boolean = DFS.ifAny(
    listOf(this),
    {
        when (val classifier = it.classifierOrNull) {
            is IrTypeParameterSymbol -> classifier.owner.superTypes
            is IrClassSymbol -> emptyList()
            null -> emptyList()
            else -> error("Unsupported classifier: $classifier")
        }
    }, {
        when (it) {
            is IrSimpleType -> it.hasQuestionMark
            else -> it is IrDynamicType
        }
    }
)
