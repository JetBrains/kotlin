// ISSUE: KT-68618

val Int.fooA: Int get() = this@fooA

val Int.fooB get() = this@fooB

fun Int.fooC(): Int = this@fooC

fun Int.fooD() = this@fooD
