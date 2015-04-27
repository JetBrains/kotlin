// KT-6822 Smart cast doesn't work inside local returned expression in lambda

val a : (Int?) -> Int = l@ {
    if (it != null) return@l <!DEBUG_INFO_SMARTCAST!>it<!>
    5
}

fun <R> let(<!UNUSED_PARAMETER!>f<!>: (Int?) -> R): R = null!!

val b: Int = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>let<!> {
    if (it != null) return@let it
    5
}

val c: Int = let {
    if (it != null) <!DEBUG_INFO_SMARTCAST!>it<!> else 5
}
