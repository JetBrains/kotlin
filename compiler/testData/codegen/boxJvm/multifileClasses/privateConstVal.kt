// TARGET_BACKEND: JVM
// WITH_STDLIB
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
