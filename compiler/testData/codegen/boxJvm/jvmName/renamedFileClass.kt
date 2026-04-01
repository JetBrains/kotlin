// TARGET_BACKEND: JVM

// WITH_STDLIB

@file:JvmName("Util")
package test

fun foo(): String = bar()
fun bar(): String = qux()
fun qux(): String = "OK"

fun box(): String = foo()
