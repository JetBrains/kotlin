// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt

@file:[JvmName("Test") JvmMultifileClass]

typealias S = String
typealias LS = List<S>

// MODULE: main(lib)
// FILE: B.kt

import java.util.Arrays

fun box(): S {
    val l: LS = Arrays.asList("OK")
    return l[0]
}
