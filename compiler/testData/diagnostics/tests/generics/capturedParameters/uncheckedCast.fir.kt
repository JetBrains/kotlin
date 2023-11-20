fun <E> foo(x: Any, y: Any) : Any {
    class C
    // without E?
    if (x is <!CANNOT_CHECK_FOR_ERASED!>C<!>) {
        return x
    }

    if (1 == 2) {
        x <!UNCHECKED_CAST!>as C<!>
    }

    if (2 == 3) {
        x <!UNCHECKED_CAST!>as? C<!>
    }

    class Outer<F> {
        inner class Inner
    }

    // bare type
    if (y is <!NO_TYPE_ARGUMENTS_ON_RHS!>Outer<!>) {
        return y
    }

    if (y is <!CANNOT_CHECK_FOR_ERASED!>Outer<*><!>) {
        return y
    }

    if (y is <!NO_TYPE_ARGUMENTS_ON_RHS!>Outer.Inner<!>) {
        return y
    }

    if (y is <!CANNOT_CHECK_FOR_ERASED!>Outer<*>.Inner<!>) {
        return y
    }

    y <!UNCHECKED_CAST!>as Outer<*><!>
    y as <!NO_TYPE_ARGUMENTS_ON_RHS!>Outer<!>

    y <!UNCHECKED_CAST!>as Outer<*>.Inner<!>
    y as <!NO_TYPE_ARGUMENTS_ON_RHS!>Outer.Inner<!>

    return C()
}

fun noTypeParameters(x: Any) : Any {
    class C
    if(x is C) {
        return x
    }

    return C()
}
