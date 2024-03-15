// FIR_IDENTICAL
// ISSUE: KT-62866
// WITH_STDLIB
// FIR_DUMP

class Foo {
    object Bar {
        override fun toString(): String = "object Bar"
    }

    companion object {
        val Bar = 10
    }
}

fun <T> take(it: T): T = it

val x = Foo.Bar            // K1: val Bar, K2: object Bar
val y = take(Foo.Bar)      // K1: val Bar, K2: object Bar
val z = Foo.Bar.let { it } // K1 & K2: object Bar
