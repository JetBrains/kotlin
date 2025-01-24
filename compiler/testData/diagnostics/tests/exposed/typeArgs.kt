// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// LATEST_LV_DIFFERENCE

// FILE: case1.kt
internal open class My

abstract class Your {
    // invalid, List<My> is effectively internal
    abstract fun <!EXPOSED_FUNCTION_RETURN_TYPE!>give<!>(): List<My>
}

// invalid, List<My> is effectively internal
interface His: <!EXPOSED_SUPER_INTERFACE!>List<My><!>

// invalid, My is internal
interface Generic<<!EXPOSED_TYPE_PARAMETER_BOUND!>E: My<!>>

interface Our {
    // invalid, Generic<My> is effectively internal
    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>(): Generic<*>
}

// FILE: case2.kt

internal interface Inter {
    fun foo() = 10
}

class Wrapper<T>(val it: T)

fun <T: Inter?> public(a: T & Any) = Wrapper(a)

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>other<!>() = public(object : Inter {})
