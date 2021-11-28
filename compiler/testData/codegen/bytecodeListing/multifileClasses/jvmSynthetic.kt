// WITH_STDLIB
// FILE: f.kt

@file:JvmName("Foo")
@file:JvmMultifileClass
@file:JvmSynthetic
package test

fun f() {}

// FILE: g.kt

@file:JvmName("Foo")
@file:JvmMultifileClass
@file:JvmSynthetic
package test

val g = ""
