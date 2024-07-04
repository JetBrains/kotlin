// IGNORE_BACKEND: ANY

// MODULE: lib1
// FILE: file1.kt
package org.sample

private class Private()
private val a1 = Private()

internal inline fun isPrivate1(obj: Any = a1): String = when (obj) {
    is Private -> "OK1"
    else -> "FAIL1"
}

// MODULE: lib2
// FILE: file2.kt
package org.sample

private class Private()
private val a2 = Private()

internal inline fun isPrivate2(obj: Any = a2): String = when (obj) {
    is Private -> "OK2"
    else -> "FAIL2"
}

// MODULE: main()(lib1, lib2)
// FILE: main.kt
import org.sample.*

fun box(): String {
    var result = ""
    result += isPrivate1()
    result += isPrivate2()
    if (result != "OK1OK2") return result
    return "OK"
}
