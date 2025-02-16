// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-27112, KTLC-275
// LANGUAGE: +ReportExposedTypeForMoreCasesOfTypeParameterBounds -ReportExposedTypeForInternalTypeParameterBounds

// FILE: Foo.kt
private open class Foo {
    fun bar() {}
}

fun <T : <!EXPOSED_TYPE_PARAMETER_BOUND!>Foo<!>> foo(x: T?) = x

open class Box<T, K>
internal open class Bar

fun <T : <!EXPOSED_TYPE_PARAMETER_BOUND_DEPRECATION_WARNING!>Box<Unit, Bar><!>> bar(x: T?) = x
fun <T : <!EXPOSED_TYPE_PARAMETER_BOUND!>Box<Bar, Foo><!>> baz(x: T?) = x

