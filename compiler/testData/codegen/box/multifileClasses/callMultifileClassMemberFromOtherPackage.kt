// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: box.kt

package test

import b.bar

fun box(): String = bar()

// FILE: caller.kt

package b

import a.foo

fun bar(): String = foo()

// FILE: multifileClass.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

fun foo(): String = "OK"
