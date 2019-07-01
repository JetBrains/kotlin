// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// WITH_RUNTIME
// !INHERIT_MULTIFILE_PARTS
// FILE: box.kt

import a.*

fun box(): String = OK.okRef.get()

// FILE: part1.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

private const val ok = "OK"

object OK {
    val okRef = ::ok
}
