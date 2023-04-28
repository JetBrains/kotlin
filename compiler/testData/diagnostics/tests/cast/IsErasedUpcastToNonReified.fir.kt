fun <T, S : T> test(x: T?, y: S, z: T) {
    x is <!CANNOT_CHECK_FOR_ERASED!>T<!>
    <!USELESS_IS_CHECK!>x is T?<!>

    <!USELESS_IS_CHECK!>y is T<!>
    <!USELESS_IS_CHECK!>y is S<!>
    <!USELESS_IS_CHECK!>y is T?<!>
    <!USELESS_IS_CHECK!>y is S?<!>

    <!USELESS_IS_CHECK!>z is T<!>
    <!USELESS_IS_CHECK!>z is T?<!>

    null <!UNCHECKED_CAST!>as T<!>
    null as T?
    null <!UNCHECKED_CAST!>as S<!>
}

class Box<T>

inline fun <reified T> test(x: T?, a: Any) {
    x is T
    null as T
    null as T?

    a is T
    a as T

    a is <!CANNOT_CHECK_FOR_ERASED!>Box<T><!>
    a is <!CANNOT_CHECK_FOR_ERASED!>Array<T><!>
    a <!UNCHECKED_CAST!>as Box<T><!>
    a <!UNCHECKED_CAST!>as Array<T><!>

    a is <!CANNOT_CHECK_FOR_ERASED!>Box<List<T>><!>
    a is <!CANNOT_CHECK_FOR_ERASED!>Array<List<T>><!>
    a <!UNCHECKED_CAST!>as Box<List<T>><!>
    a <!UNCHECKED_CAST!>as Array<List<T>><!>
}

fun <T> foo(x: List<T>, y: List<T>?) {
    <!USELESS_IS_CHECK!>x is List<T><!>
    y is List<T>
}
