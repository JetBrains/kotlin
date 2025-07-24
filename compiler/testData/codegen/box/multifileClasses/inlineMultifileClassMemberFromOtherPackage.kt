// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR_SERIALIZE
// WITH_STDLIB
// FILE: box.kt

package test

import a.foo

fun box(): String = foo { "OK" }

// FILE: foo.kt

@file:[JvmName("A") JvmMultifileClass]
package a

inline fun foo(body: () -> String): String = zee(body())

// FILE: zee.kt

@file:[JvmName("A") JvmMultifileClass]
package a

public fun zee(x: String): String = x
