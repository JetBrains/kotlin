// !WITH_NEW_INFERENCE
// KT-6822 Smart cast doesn't work inside local returned expression in lambda 

val a /* :(Int?) -> Int? */ = l@ { it: Int? -> // but must be (Int?) -> Int
    if (it != null) return@l it
    5
}

fun <R> let(<!UNUSED_PARAMETER!>f<!>: (Int?) -> R): R = null!!

val b /*: Int? */ = let { // but must be Int
    if (it != null) return@let it
    5
}

val c /*: Int*/ = let {
    if (it != null) <!DEBUG_INFO_SMARTCAST!>it<!> else 5
}