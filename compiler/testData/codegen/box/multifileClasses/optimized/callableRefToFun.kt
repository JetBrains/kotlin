// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// WITH_STDLIB
// !INHERIT_MULTIFILE_PARTS
// FILE: box.kt

import a.*

fun box(): String = (::ok).let { it() }

// FILE: part1.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

fun ok() = "OK"
