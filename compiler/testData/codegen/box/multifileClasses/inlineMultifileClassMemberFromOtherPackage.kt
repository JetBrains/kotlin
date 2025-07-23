// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_SPLITTING_TO_TWO_MODULES: ANY
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
