// JVM_ABI_K1_K2_DIFF: KT-63984

// MODULE: lib
// FILE: A.kt
package test

var property = "fail"
    private set

fun test() {
    property = "OK"
}

// MODULE: main(lib)
// FILE: B.kt

import test.*

fun box(): String {
    test()
    return property
}
