// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME
// KOTLIN_CONFIGURATION_FLAGS: +JVM.INHERIT_MULTIFILE_PARTS
// FILE: box.kt

import a.*

fun box(): String = ok()

// FILE: part1.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

private val overlapping = run { "oops #1" }

// FILE: part2.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

private val overlapping = run { "OK" }

fun ok() = overlapping

// FILE: part3.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

private val overlapping = run { "oops #2" }
