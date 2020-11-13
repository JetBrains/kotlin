// KOTLIN_CONFIGURATION_FLAGS: +JVM.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
// FILE: 1.kt
package test

inline class IC(val i: Int)

// FILE: 2.kt
package kotlin

import test.*

fun foo(i: Int, ic: IC) {}