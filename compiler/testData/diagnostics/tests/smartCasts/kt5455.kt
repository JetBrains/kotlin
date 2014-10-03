//KT-5455 Need warning about redundant type cast
fun foo(o: Any): Int {
    if (o is String) {
        return (o <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as<!> String).length
    }
    return -1
}

open class A {
    fun foo() {}
}
class B: A()

fun test(a: Any?) {
    if (a is B) {
        (a <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as<!> A).foo()
    }
}

fun test1(a: B) {
    (a <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as<!> A?)?.foo()
}

fun test2(b: B?) {
    if (b != null) {
        (b <!USELESS_CAST_STATIC_ASSERT_IS_FINE!>as<!> A).foo()
    }
}
