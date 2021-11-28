// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// WITH_STDLIB
// !INHERIT_MULTIFILE_PARTS
// FILE: box.kt

import a.*

fun box(): String = okInline()

// FILE: part1.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

internal const val ok = "OK"

internal inline fun okInline() =
        ::ok.get()
