class MyList<T>

fun ff(a: Any) = a is <!CANNOT_CHECK_FOR_ERASED!>MyList<String><!>