//For testing error messages text see DiagnosticMessageTest.testFunctionPlaceholder
package a

class A<T, R>
fun <T, R> foo(a: A<T, R>) = a
fun <T, R> bar(f: (T) -> R) = f

fun test() {
    foo <!TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!> }<!>
    foo <!TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!>}<!>
    foo <!TYPE_MISMATCH!>{ x: Int -> x}<!>

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> { <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> 1 }
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> 1}
    bar { x: Int -> x + 1}
}
