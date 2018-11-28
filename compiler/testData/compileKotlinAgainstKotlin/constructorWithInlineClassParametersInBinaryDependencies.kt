// !LANGUAGE: +InlineClasses
// FILE: A.kt
package lib

inline class S(val string: String)

class Test(val s: S)

// FILE: B.kt
import lib.*

fun box() = Test(S("OK")).s.string