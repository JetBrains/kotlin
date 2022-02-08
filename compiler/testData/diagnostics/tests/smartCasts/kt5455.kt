// FIR_IDENTICAL
//KT-5455 Need warning about redundant type cast
fun foo(o: Any): Int {
    if (o is String) {
        return (o <!USELESS_CAST!>as String<!>).length
    }
    return -1
}

open class A {
    fun foo() {}
}
class B: A()

fun test(a: Any?) {
    if (a is B) {
        (a as A).foo()
    }
}

fun test1(a: B) {
    (a as A?)?.foo()
}

fun test2(b: B?) {
    if (b != null) {
        (b as A).foo()
    }
}
