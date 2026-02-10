// FILE: lib.kt
package foo

var i = 0

inline fun f() = i * 2

// FILE: main.kt
import foo.*

fun box(): String {
    return if ((++i + f()) == 3) "OK" else "fail"
}