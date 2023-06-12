// TARGET_BACKEND: JVM
// WITH_STDLIB
// !INHERIT_MULTIFILE_PARTS
// FILE: box.kt

import a.*

fun box(): String = ::OK.get()

// FILE: part1.kt
@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

const val OK = "OK"
