// TARGET_BACKEND: JVM
// !LANGUAGE: +InlineClasses
// FILE: A.kt
// KOTLIN_CONFIGURATION_FLAGS: +JVM.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
package lib

inline class S(val string: String)

class Test(val s: S)

// FILE: B.kt
import lib.*

fun box() = Test(S("OK")).s.string