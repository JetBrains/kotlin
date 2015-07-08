//For testing error messages text see DiagnosticMessageTest.testFunctionPlaceholder
package a

class A<T, R>
fun <T, R> foo(a: A<T, R>) = a
fun <T, R> bar(f: (T) -> R) = f

fun test() {
    <!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>foo<!> <!TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!> }<!>
    <!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>foo<!> <!TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>}<!>
    <!TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>foo<!> <!TYPE_MISMATCH!>{ x: Int -> x}<!>

    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> { <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> 1 }
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> 1}
    bar { x: Int -> x + 1}
}