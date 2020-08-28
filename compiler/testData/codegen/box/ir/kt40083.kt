// MODULE: lib
// FILE: lib.kt

// KT-40083

open class X {
    open fun red(x: Int, y: (() -> Int) = { 31 }): Int {
        return x+y()
    }
}

class Y: X()

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    if (Y().red(17) { 23 } != 40) return "FAIL 1"
    if (Y().red(29) != 60) return "FAIL 2"
    return "OK"
}

