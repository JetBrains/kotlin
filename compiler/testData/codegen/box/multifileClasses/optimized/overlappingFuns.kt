// TARGET_BACKEND: JVM
// WITH_STDLIB
// !INHERIT_MULTIFILE_PARTS
// FILE: box.kt

import a.*

fun box(): String = ok()

// FILE: part1.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

private fun overlapping() = "oops #1"

// FILE: part2.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

private fun overlapping() = "OK"

fun ok() = overlapping()

// FILE: part3.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

private fun overlapping() = "oops #2"
