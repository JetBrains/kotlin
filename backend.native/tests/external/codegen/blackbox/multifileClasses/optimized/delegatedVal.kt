// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// KOTLIN_CONFIGURATION_FLAGS: +JVM.INHERIT_MULTIFILE_PARTS
// FILE: box.kt

import a.*

fun box(): String = OK

// FILE: part1.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

val OK: String by lazy { "OK" }