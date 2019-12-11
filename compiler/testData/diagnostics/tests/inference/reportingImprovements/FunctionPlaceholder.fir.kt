// !WITH_NEW_INFERENCE
//For testing error messages text see DiagnosticMessageTest.testFunctionPlaceholder
package a

class A<T, R>
fun <T, R> foo(a: A<T, R>) = a
fun <T, R> bar(f: (T) -> R) = f

fun test() {
    <!INAPPLICABLE_CANDIDATE!>foo<!> { <!UNRESOLVED_REFERENCE!>it<!> }
    <!INAPPLICABLE_CANDIDATE!>foo<!> { x -> x}
    <!INAPPLICABLE_CANDIDATE!>foo<!> { x: Int -> x}

    bar { it <!UNRESOLVED_REFERENCE!>+<!> 1 }
    bar { x -> x <!UNRESOLVED_REFERENCE!>+<!> 1}
    bar { x: Int -> x + 1}
}