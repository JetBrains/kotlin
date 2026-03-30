// MODULE: lib
// FILE: lib.kt
package lib

open class Props {
    val a: Int = 0
    val b: String = ""
}

fun leaf(...Props.$props) {}

fun mid(...leaf.$props) {}

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.mid

fun target(...mid.$props) {
    <expr>a</expr>
    b.length
}
