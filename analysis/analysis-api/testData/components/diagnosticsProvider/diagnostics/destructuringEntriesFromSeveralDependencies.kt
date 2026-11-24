// KT-90376
// MODULE: lib
// FILE: declarations.kt
package lib

data class X(val a: Int, val b: String)

val (a, b) = X(1, "")

val c = 3

// MODULE: script
// MODULE_KIND: ScriptSource
// FILE: declarations.kts
data class Y(val a: Int, val b: String)

val (d, e) = Y(1, "")

val f = 3

// MODULE: other
// FILE: other.kt
package other

val g = 4

// MODULE: main(lib, script, other)
// FILE: main.kt
import lib.*

fun use() {
    a
    b
    c
    d
    e
    f
}
