// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// RENDER_DIAGNOSTICS_FULL_TEXT
internal open class My

abstract class Your {
    // invalid, List<My> is effectively internal
    abstract fun <!EXPOSED_FUNCTION_RETURN_TYPE!>give<!>(): List<My>
}

// invalid, List<My> is effectively internal
interface His: <!EXPOSED_SUPER_INTERFACE!>List<My><!>

// invalid, My is internal
interface Generic<E: <!EXPOSED_TYPE_PARAMETER_BOUND!>My<!>>

interface Our {
    // invalid, Generic<My> is effectively internal
    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>(): Generic<*>
}

// MODULE: a

internal interface Inter {
    fun foo() = 10
}

class Wrapper<T>(val it: T)

fun <T: <!EXPOSED_TYPE_PARAMETER_BOUND!>Inter?<!>> public(a: T & Any) = Wrapper(a)

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>other<!>() = public(object : Inter {})

// MODULE: b(a)

fun test() {
    other().it.<!INVISIBLE_REFERENCE!>foo<!>() // ok in K1, invisible reference in K2
}
