// TARGET_BACKEND: JVM
// LANGUAGE: +ProperInternalVisibilityCheckInImportingScope
// See also: KT-23727
// WITH_STDLIB
// MODULE: m1
// FILE: Foo.kt

package foo

internal annotation class Volatile

// MODULE: m2(m1)
// FILE: Bar.kt

import foo.*

class Bar {
    @Volatile
    var v = 0
}

fun box(): String {
    val bar = Bar()
    return if (bar.v == 0) "OK" else "FAIL: ${bar.v}"
}
