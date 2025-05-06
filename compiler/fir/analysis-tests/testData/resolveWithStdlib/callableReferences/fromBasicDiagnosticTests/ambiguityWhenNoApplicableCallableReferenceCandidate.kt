// RUN_PIPELINE_TILL: FRONTEND
fun foo(x: Int) {}
fun foo(y: String) {}

fun <T> bar(f: (T) -> Unit) {}

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>bar<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>)
}
