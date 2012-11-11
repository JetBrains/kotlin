fun f(a: List<Hashable>) = a is <!CANNOT_CHECK_FOR_ERASED!>List<Number><!>
