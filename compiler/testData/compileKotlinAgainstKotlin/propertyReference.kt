// FILE: A.kt

package a

public var topLevel: Int = 42

public val String.extension: Long
    get() = length.toLong()

// FILE: B.kt

import a.*

fun box(): String {
    val f = ::topLevel
    val x1 = f.get()
    if (x1 != 42) return "Fail x1: $x1"
    f.set(239)
    val x2 = f.get()
    if (x2 != 239) return "Fail x2: $x2"

    val g = String::extension
    val y1 = g.get("abcde")
    if (y1 != 5L) return "Fail y1: $y1"

    return "OK"
}
