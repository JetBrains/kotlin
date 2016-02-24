// FILE: A.kt

package lib

@JvmName("bar")
fun foo() = "foo"

var v: Int = 1
    @JvmName("vget")
    get
    @JvmName("vset")
    set

// FILE: B.kt

import lib.*

fun main(args: Array<String>) {
    foo()

    v = 1
    println(v)
}
