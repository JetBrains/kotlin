// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// WITH_RUNTIME
// KOTLIN_CONFIGURATION_FLAGS: +JVM.INHERIT_MULTIFILE_PARTS
// FILE: box.kt

import a.*

fun box(): String = okInline()

// FILE: part1.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

internal val ok = run { "OK" }

internal inline fun okInline() =
        ::ok.get()
