// ISSUE: KT-52424

interface A

fun foo(testFun: ((Int) -> Unit)?, anyInterface: A?) {}

fun test(x: Int?) {
    foo(
        if (x != null) { { 0 } } else null,
        <!ARGUMENT_TYPE_MISMATCH!>x<!> // should be no smartcast to Nothing?
    )
}
