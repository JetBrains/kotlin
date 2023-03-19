// ISSUE: KT-27261
// WITH_STDLIB

import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
infix fun Boolean.takeRight(cond: Boolean): Boolean {
    contract { returns(true) implies cond }
    return cond
}

@OptIn(ExperimentalContracts::class)
infix fun Boolean?.ensureLeft(x: Any): Boolean {
    contract { returns(true) implies (this@ensureLeft != null) }
    return this != null
}

fun test_1(b: Boolean, x: Any) {
    if (b takeRight (x is String)) {
        x.length
    }
}

fun test_2(b: Boolean?, x: Any) {
    if (b ensureLeft (x is String)) {
        b.not()
    }
}
