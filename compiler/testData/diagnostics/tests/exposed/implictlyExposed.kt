// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// LATEST_LV_DIFFERENCE

internal interface Inter {
    fun foo() = 10
}

class Wrapper<T>(val it: T)

fun <T: Inter?> public(a: T & Any) = Wrapper(a)

fun <!EXPOSED_FUNCTION_RETURN_TYPE!>other<!>() = public(object : Inter {})
