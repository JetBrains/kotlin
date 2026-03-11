// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: 1/part.kt

@file:JvmName("Foo")
@file:JvmMultifileClass
package test

fun foo(): String = "O"

// FILE: 2/part.kt

@file:JvmName("Bar")
@file:JvmMultifileClass
package test

fun bar(): String = "K"

// FILE: box.kt

package test

fun box(): String = foo() + bar()
