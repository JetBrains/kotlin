// FILE: main.kt
package test

import java.util.function.Supplier

fun interface IntSupplier {
    fun get(): Int
}

class Box(val value: Int)

val topLevelProperty: Int = 42

fun takeIntSupplier(supplier: IntSupplier): Int = supplier.get()

fun takeSupplier(supplier: Supplier<Int>): Int = supplier.get()

// The SAM conversions of the property references become rich function references to their getters.
fun testKotlinFunInterface(): Int = takeIntSupplier(::topLevelProperty)

fun testJavaInterface(): Int = takeSupplier(::topLevelProperty)

fun testBoundReference(box: Box): Int = takeIntSupplier(box::value)
