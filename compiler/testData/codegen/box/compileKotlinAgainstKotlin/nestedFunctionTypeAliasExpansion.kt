// MODULE: lib
// FILE: lib.kt

package lib

typealias Dispatch<Msg> = (Msg) -> Unit
typealias Effect<Msg> = (Dispatch<Msg>) -> Unit

fun <Msg> noEffect(): Effect<Msg> = TODO()

// MODULE: main(lib)
// FILE: main.kt

import lib.*

fun box(): String {
    val s = { noEffect<Unit>() }
    return "OK"
}
