fun <T, S : T> test(x: T?, y: S, z: T) {
    x is <!CANNOT_CHECK_FOR_ERASED!>T<!>
    x is T?

    y is T
    y is S
    y is T?
    y is S?

    z is T
    z is T?

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
    x is List<T>
    y is List<T>
}