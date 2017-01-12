// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

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
