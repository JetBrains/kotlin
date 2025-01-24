// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-27112, KTLC-275
// LANGUAGE: +ReportExposedTypeForMoreCasesOfTypeParameterBounds -ReportExposedTypeForInternalTypeParameterBounds

// FILE: Foo.kt
private open class Foo {
    fun bar() {}
}

fun <T : Foo> foo(x: T?) = x

open class Box<T, K>
internal open class Bar

fun <T : Box<Unit, Bar>> bar(x: T?) = x
fun <T : Box<Bar, Foo>> baz(x: T?) = x

