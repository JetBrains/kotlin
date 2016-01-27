// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE


class A

fun A.foo() = this

fun test(a: A) {
    fun A.foo() = 3

    a.foo() checkType { _<Int>() }
    with(a) {
        foo() checkType { _<Int>() }
    }
}
