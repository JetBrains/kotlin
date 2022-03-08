// TARGET_BACKEND: JVM
// WITH_STDLIB
// MODULE: lib
// USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
// FILE: A.kt

package a

@Suppress("UNSUPPORTED_FEATURE")
inline class Foo(val x: IntArray) {
    val size: Int get() = x.size
}

// MODULE: main(lib)
// FILE: B.kt

import a.*

fun box(): String {
    val a = Foo(intArrayOf(3, 4))
    if (a.size != 2) return "Fail"
    return "OK"
}
