// TARGET_BACKEND: JVM
// TODO: KT-37972 IllegalAccessError on initializing property reference for a property declared in JvmMultifileClass with -Xmultifile-parts-inherit
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// !INHERIT_MULTIFILE_PARTS
// FILE: box.kt

import a.propRefA
import b.propRefB

fun box(): String {
    if (propRefA != propRefB) return "Fail: propRefA != propRefB"
    return "OK"
}

// FILE: a.kt

package a

import test.property

val propRefA = ::property

// FILE: b.kt

package b

import test.property

val propRefB = ::property

// FILE: part.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package test

val property: String get() = ""
