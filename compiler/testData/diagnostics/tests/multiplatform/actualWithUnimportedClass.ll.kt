// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-68819
// MODULE: m1-common
// FILE: common.kt
package pkg

class Foo

// FILE: common2.kt
import pkg.Foo

expect fun foo(f: Foo)
expect fun bar(f: List<Foo>)
// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual fun foo(f: <!UNRESOLVED_REFERENCE!>Foo<!>) {}
actual fun bar(f: List<<!UNRESOLVED_REFERENCE!>Foo<!>>) {}

/* GENERATED_FIR_TAGS: actual, classDeclaration, expect, functionDeclaration */
