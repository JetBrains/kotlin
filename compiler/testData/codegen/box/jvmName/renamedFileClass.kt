// TARGET_BACKEND: JVM

// WITH_RUNTIME

@file:JvmName("Util")
package test

fun foo(): String = bar()
fun bar(): String = qux()
fun qux(): String = "OK"

fun box(): String = foo()
