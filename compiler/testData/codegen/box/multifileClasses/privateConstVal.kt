// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: foo.kt
@file:JvmName("Util")
@file:JvmMultifileClass
package test

private const val x = "O"

fun foo() = x

// FILE: bar.kt
@file:JvmName("Util")
@file:JvmMultifileClass
package test

private const val x = "K"

fun bar() = x

// FILE: test.kt
package test

fun box(): String = foo() + bar()
