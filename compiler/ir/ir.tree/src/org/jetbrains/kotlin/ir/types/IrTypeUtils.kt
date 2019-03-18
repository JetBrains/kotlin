/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.symbols.*
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

fun IrType.isEqualTo(that: IrType): Boolean {
    if (this is IrDynamicType && that is IrDynamicType) return true
    if (this is IrErrorType || that is IrErrorType) return false
    if (this === that) return true
    if (this is IrSimpleType && that is IrSimpleType) return FqNameEqualityChecker.areEqual(this.classifier, that.classifier) &&
            this.arguments.zip(that.arguments).all { (ths, tht) ->
                when (ths) {
                    is IrStarProjection -> tht is IrStarProjection
                    is IrTypeProjection -> tht is IrTypeProjection
                            && ths.variance == tht.variance
                            && ths.type.isEqualTo(tht.type)
                    else -> error("Unsupported Type Argument")
                }
            }
    return false
}

fun IrTypeArgument.toHashCode(): Int = when (this) {
    is IrTypeProjection -> 31 * type.toHashCode() + variance.hashCode()
    is IrStarProjection -> hashCode()
    else -> 0
}

fun IrType.toHashCode(): Int {
    if (this is IrDynamicType) return -1
    if (this is IrErrorType) return 0

    require(this is IrSimpleType)

    var result = classifier.hashCode()

    result = 31 * result + arguments.fold(0) { a, t -> 31 * a + t.toHashCode() }
    return 31 * result + if (hasQuestionMark) 1 else 0
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