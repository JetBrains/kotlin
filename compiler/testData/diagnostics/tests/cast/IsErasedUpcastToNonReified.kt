fun <T, S : T> test(x: T?, y: S, z: T) {
    x is <!CANNOT_CHECK_FOR_ERASED!>T<!>
    <!USELESS_IS_CHECK!>x is T?<!>

    <!USELESS_IS_CHECK!>y is T<!>
    <!USELESS_IS_CHECK!>y is S<!>
    <!USELESS_IS_CHECK!>y is T?<!>
    <!USELESS_IS_CHECK!>y is S?<!>

    <!USELESS_IS_CHECK!>z is T<!>
    <!USELESS_IS_CHECK!>z is T?<!>

    <!UNCHECKED_CAST!>null as T<!>
    null <!USELESS_CAST!>as T?<!>
    <!UNCHECKED_CAST!>null as S<!>
}

inline fun <reified T> test(x: T?) {
    x is T
    null as T
    null <!USELESS_CAST!>as T?<!>
}

fun <T> foo(x: List<T>, y: List<T>?) {
    <!USELESS_IS_CHECK!>x is List<T><!>
    y is List<T>
}