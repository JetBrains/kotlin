// WITH_STDLIB
// CHECK_BYTECODE_TEXT
// FILE: lambdas.kt
// lambdas, capturing lambdas and bound references are invoked directly, so they are inlined by DirectInvokeLowering
class Scale(val factor: Int) {
    fun apply(x: Int): Int = x * factor
}

fun lambdas(items: List<Int>): List<Int> {
    val limit = 1
    val scale = Scale(10)
    return items.asSequence().filter { it > limit }.map(scale::apply).map { it + 1 }.toList()
}

// @LambdasKt.class:
// 0 kotlin/jvm/functions/Function1.invoke
// 0 INVOKEDYNAMIC
// 0 kotlin/sequences/SequencesKt

// FILE: variable.kt
// a function value stored in a variable is invoked through the `Function1` interface for every element
fun variable(items: List<Int>): List<Int> {
    val keep: (Int) -> Boolean = { it > 1 }
    return items.asSequence().filter(keep).toList()
}

// @VariableKt.class:
// 1 kotlin/jvm/functions/Function1.invoke
// 0 kotlin/sequences/SequencesKt

// FILE: property.kt
// a property reference is not a function reference, so the sequence is not fused
class Box(val value: Int)

fun property(items: List<Box>): List<Int> = items.asSequence().map(Box::value).toList()

// @PropertyKt.class:
// 1 kotlin/sequences/SequencesKt.map
// 1 kotlin/sequences/SequencesKt.toList

// FILE: box.kt
fun box(): String {
    val a = lambdas(listOf(1, 2, 3))
    if (a != listOf(21, 31)) return "Fail lambdas: $a"
    val b = variable(listOf(1, 2, 3))
    if (b != listOf(2, 3)) return "Fail variable: $b"
    val c = property(listOf(Box(1), Box(2)))
    if (c != listOf(1, 2)) return "Fail property: $c"
    return "OK"
}
