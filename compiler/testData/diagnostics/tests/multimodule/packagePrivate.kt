// RUN_PIPELINE_TILL: FRONTEND
// MODULE: m1
// FILE: a.kt

package p

private val a = 1

// MODULE: m2(m1)
// FILE: c.kt

package p

val c = <!INVISIBLE_MEMBER!>a<!> // same package, another module
