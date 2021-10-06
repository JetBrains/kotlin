fun <E> foo(x: Any, y: Any) : Any {
    class C
    // without E?
    if(x is C) {
        return x
    }

    if (1 == 2) {
        x as C
    }

    if (2 == 3) {
        x as? C
    }

    class Outer<F> {
        inner class Inner
    }

    // bare type
    if (y is <!NO_TYPE_ARGUMENTS_ON_RHS!>Outer.Inner<!>) {
        return y
    }

    y as Outer<*>.Inner

    return C()
}

fun noTypeParameters(x: Any) : Any {
    class C
    if(x is C) {
        return x
    }

    return C()
}
