// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// MODULE: m1
// FILE: a.kt

package p

public fun foo(a: Int) {}
public fun foo(vararg values: Int) {}

// MODULE: m2
// FILE: b.kt

package p

public fun foo(a: Int) {}
public fun foo(vararg values: Int) {}

// MODULE: m3(m1, m2)
// FILE: c.kt
package m

import p.foo

fun main() {
    foo(12)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, vararg */
