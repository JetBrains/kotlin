// LANGUAGE_VERSION: 1.3
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package test

import kotlin.internal.contracts.*

class A

fun simpleIsInstace(x: Any?) {
    contract {
        returns(true) implies (x is A)
    }
}

fun Any?.receiverIsInstance() {
    contract {
        returns(true) implies (this@receiverIsInstance is A)
    }
}
