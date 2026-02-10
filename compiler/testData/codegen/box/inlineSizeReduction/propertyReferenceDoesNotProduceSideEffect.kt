// FILE: lib.kt
package foo

var log = ""

fun bar(a: Int, b: Int) {
    log += "bar($a, $b);"
}

inline fun test(a: Int, b: Int) {
    bar(b, a)
}

// FILE: main.kt
package foo
import kotlin.test.*

class A() {
    var x = 23
}

fun sideEffect(): Int {
    log += "sideEffect();"
    return 42
}

fun box(): String {
    val a = A()
    test(sideEffect(), a.x)
    assertEquals("sideEffect();bar(23, 42);", log)
    return "OK"
}