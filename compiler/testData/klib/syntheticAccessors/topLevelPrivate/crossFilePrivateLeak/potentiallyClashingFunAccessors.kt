// IGNORE_BACKEND: NATIVE

// FILE: file1.kt
package org.sample

private fun fileName() = "file1.kt "

internal inline fun inlineFun1() = fileName()

// FILE: file2.kt
package org.sample

private fun fileName() = "file2.kt "

internal inline fun inlineFun2() = fileName()

// FILE: main.kt
import org.sample.*

fun box(): String {
    var result = ""
    result += inlineFun1()
    result += inlineFun2()
    if (result != "file1.kt file2.kt ") return result
    return "OK"
}
