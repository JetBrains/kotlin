// IGNORE_BACKEND: NATIVE

// FILE: O.kt

package example

abstract class O<T> {
    open fun <E : T> min(a: E, b: E, c: E, vararg rest: E): E {
        null!!
    }
}

// FILE: RNO.kt

package example

import kotlin.Comparable

class RNO : O<Comparable<*>> {
    private constructor()

    override fun <E : Comparable<*>> min(a: E, b: E, c: E, vararg rest: E): E {
        null!!
    }
}

// FILE: main.kt

package example

fun box(): String {
    return "OK"
}