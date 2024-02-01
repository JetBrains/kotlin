// !LANGUAGE: +ContextReceivers
// ISSUE: KT-64531, KT-64488
// FIR_DUMP
// WITH_STDLIB

// FILE: a.kt

package a

val y: Int = 0

// FILE: b.kt

package b

val z: Int = 0

// FILE: c.kt

package c

import a.y
import b.*
import c.Foo.*

val x: Int = 0

enum class Foo { A, B, C }

class Bar(val x: String, val y: String, val z: String, val w: String, val entries: String)

class Baz(val w: Int)

context(Bar)
fun test1() = x

context(Bar)
fun test2() = y

context(Bar)
fun test3() = z

context(Bar)
fun test4() = <!DEPRECATED_ACCESS_TO_ENTRY_PROPERTY_FROM_ENUM!>entries<!>

context(Bar)
fun Baz.test5() = w
