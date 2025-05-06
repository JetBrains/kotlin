// RUN_PIPELINE_TILL: FRONTEND
package b

fun <T, R> foo(map: Map<T, R>) : R = throw Exception()

fun <F, G> getMap() : Map<F, G> = throw Exception()

fun bar123() {
    <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>getMap<!>(
<!SYNTAX!><!>}
