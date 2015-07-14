// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.*

class A {
    val x = 1
    fun x() {}
}

fun f1(): KProperty<Int> = A::x  // ok, property
fun f2(): (A) -> Unit = A::x     // ok, function