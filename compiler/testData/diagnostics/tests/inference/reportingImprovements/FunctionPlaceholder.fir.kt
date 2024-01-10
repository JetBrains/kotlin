//For testing error messages text see DiagnosticMessageTest.testFunctionPlaceholder
package a

class A<T, R>
fun <T, R> foo(a: A<T, R>) = a
fun <T, R> bar(f: (T) -> R) = f

fun test() {
    foo <!ARGUMENT_TYPE_MISMATCH!>{ <!UNRESOLVED_REFERENCE!>it<!> }<!>
    foo <!ARGUMENT_TYPE_MISMATCH!>{ x -> x}<!>
    foo <!ARGUMENT_TYPE_MISMATCH!>{ x: Int -> x}<!>

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> <!CANNOT_INFER_PARAMETER_TYPE!>{ it + 1 }<!>
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!> { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> x + 1}
    bar { x: Int -> x + 1}
}
