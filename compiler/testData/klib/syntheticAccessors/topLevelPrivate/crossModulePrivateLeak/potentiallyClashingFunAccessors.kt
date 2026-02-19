// MODULE: lib1
// FILE: file1.kt
package org.sample

private fun libName() = "lib1 "

internal inline fun inlineFun1() = libName()

// MODULE: lib2
// FILE: file2.kt
package org.sample

private fun libName() = "lib2 "

internal inline fun inlineFun2() = libName()

// MODULE: main()(lib1, lib2)
// FILE: main.kt
import org.sample.*

fun box(): String {
    var result = ""
    result += inlineFun1()
    result += inlineFun2()
    if (result != "lib1 lib2 ") return result
    return "OK"
}
