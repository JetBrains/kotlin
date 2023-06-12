// TARGET_BACKEND: JVM
// WITH_STDLIB
// !INHERIT_MULTIFILE_PARTS
// FILE: result.kt

@file:JvmName("Util")
@file:JvmMultifileClass
package test

fun result(): String = "OK"

// FILE: test.kt

import test.result

private inline fun id(f: () -> String): String = f()

fun box(): String = id { result() }
