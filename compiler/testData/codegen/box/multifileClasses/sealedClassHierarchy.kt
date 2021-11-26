// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:JvmMultifileClass
@file:JvmName("Test")
package test

sealed class Foo(val value: String)

class Bar : Foo("OK")

fun box(): String = Bar().value
