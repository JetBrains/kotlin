// TARGET_BACKEND: JVM
// FILE: A.kt

@file:[JvmName("Test") JvmMultifileClass]

typealias S = String
typealias LS = List<S>

// FILE: B.kt

import java.util.Arrays

fun box(): S {
    val l: LS = Arrays.asList("OK")
    return l[0]
}
