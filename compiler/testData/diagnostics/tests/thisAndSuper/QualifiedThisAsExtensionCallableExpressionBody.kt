// ISSUE: KT-68618

val Int.fooA: Int get() = this@fooA

val Int.fooB get() = this<!UNRESOLVED_REFERENCE!>@fooB<!>

fun Int.fooC(): Int = this@fooC

fun Int.fooD() = this@fooD
