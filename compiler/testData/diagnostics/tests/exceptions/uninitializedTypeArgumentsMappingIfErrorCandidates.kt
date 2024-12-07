// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73079

// FILE: class.kt

package klass

class X<T>

// FILE: typealias.kt

package alias

typealias X<P> = klass.X<P>

// FILE: main.kt

package main

import klass.<!CONFLICTING_IMPORT!>X<!>
import alias.<!CONFLICTING_IMPORT!>X<!>

fun test() {
    val x = <!UNRESOLVED_REFERENCE!>X<!><String>()
}
