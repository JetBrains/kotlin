// ISSUE: KT-51045

class Inv<T>
fun <T> invOf(): Inv<T> = Inv()

abstract class Base<T> {
    final var foo: Inv<T>? = invOf()
}

class Bar : Base<String>()

fun test_1() {
    val x = Bar()
    x.foo = invOf()
    x.foo = null
}

fun test_2(x: Base<*>) {
    x.foo = <!ASSIGNMENT_TYPE_MISMATCH!>invOf()<!>
    x.foo = null
}

fun test_3(x: Any) {
    if (x is Base<*>) {
        x.foo = <!ASSIGNMENT_TYPE_MISMATCH!>invOf()<!>
        x.foo = null
    }
}
