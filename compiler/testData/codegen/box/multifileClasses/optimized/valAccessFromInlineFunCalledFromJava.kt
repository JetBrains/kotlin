// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME
// KOTLIN_CONFIGURATION_FLAGS: +JVM.INHERIT_MULTIFILE_PARTS
// FILE: box.kt

import a.*

fun box(): String = J.ok()

// FILE: part1.kt
@file:[JvmName("MC") JvmMultifileClass]
package a

val O = run { "O" }
const val K = "K"

inline fun ok(): String {
    return O + K
}

// FILE: J.java
import a.MC;

public class J {
    public static String ok() {
        return MC.ok();
    }
}