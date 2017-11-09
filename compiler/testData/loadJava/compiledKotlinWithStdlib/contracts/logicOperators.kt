// LANGUAGE_VERSION: 1.3
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.internal.contracts.*


fun orSequence(x: Any?, y: Any?, b: Boolean) {
    contract {
        returns() implies (x is String || y is Int || !b)
    }
}

class A
class B

fun andSequence(x: Any?, y: Any?, b:Boolean) {
    contract {
        returns() implies (x is A && x is B && ((y is A) && b))
    }
}