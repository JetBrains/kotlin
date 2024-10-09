// ISSUE: KT-27112
// LANGUAGE: -ReportExposedTypeForMoreCasesOfTypeParameterBounds

// FILE: Foo.kt
private open class Foo {
    fun bar() {}
}

fun <T : <!EXPOSED_TYPE_PARAMETER_BOUND_DEPRECATION_WARNING!>Foo<!>> foo(x: T?) = x

// FILE: Main.kt
fun box() = "OK".also {
    <!IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION!>foo<!>(null)?.<!UNRESOLVED_REFERENCE!>bar<!>()
}
