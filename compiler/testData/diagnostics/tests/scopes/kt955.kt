//FILE:a.kt
//KT-955 Unable to import a Kotlin package into a Kotlin file with no package header

package foo

fun f() {}

//FILE:b.kt

import foo.*

val m = f() // unresolved

//FILE:c.kt

package java.util

fun bar() {}

//FILE:d.kt

import java.util.*

val r = bar()
