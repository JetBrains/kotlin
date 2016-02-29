// FILE: A.kt

package lib

@JvmName("bar")
fun foo() = "foo"

var v: Int = 1
    @JvmName("vget")
    get
    @JvmName("vset")
    set

fun consumeInt(x: Int) {}

class A {
    val OK: String = "OK"
        @JvmName("OK") get
}

// FILE: B.kt

import lib.*

fun box(): String {
    foo()
    v = 1
    consumeInt(v)
    return A().OK
}
