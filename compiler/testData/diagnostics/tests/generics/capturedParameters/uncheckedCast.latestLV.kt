// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
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
    if (y is <!CANNOT_CHECK_FOR_ERASED!>Outer<!>) {
        return y
    }

    if (y is <!CANNOT_CHECK_FOR_ERASED!>Outer<*><!>) {
        return y
    }

    if (y is <!CANNOT_CHECK_FOR_ERASED!>Outer.Inner<!>) {
        return y
    }

    if (y is <!CANNOT_CHECK_FOR_ERASED!>Outer<*>.Inner<!>) {
        return y
    }

    y <!UNCHECKED_CAST!>as Outer<*><!>
    y as Outer

    y <!CAST_NEVER_SUCCEEDS!>as<!> Outer<*>.Inner
    y as Outer.Inner

    return C()
}

fun noTypeParameters(x: Any) : Any {
    class C
    if(x is C) {
        return x
    }

    return C()
}
