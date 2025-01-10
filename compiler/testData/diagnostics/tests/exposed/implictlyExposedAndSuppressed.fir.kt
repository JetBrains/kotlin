// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// LANGUAGE: -ReportExposedTypeForMoreCasesOfTypeParameterBounds

// MODULE: a

internal interface Inter {
    fun foo() = 10
}

class Wrapper<T>(val it: T)

fun <T: <!EXPOSED_TYPE_PARAMETER_BOUND_DEPRECATION_WARNING!>Inter?<!>> public(a: T & Any) = Wrapper(a)

@Suppress(<!ERROR_SUPPRESSION!>"EXPOSED_FUNCTION_RETURN_TYPE"<!>)
fun other() = public(object : Inter {})

// MODULE: b(a)

fun test() {
    other().it.<!INVISIBLE_REFERENCE!>foo<!>() // ok in K1, invisible reference in K2
}
