// !DIAGNOSTICS: -UNUSED_PARAMETER, -BASE_WITH_NULLABLE_UPPER_BOUND

class A<F> {
    fun <E : F> foo1(x: E) {}
    fun <E : F?> foo2(x: E) {}

    fun <Z : F, W : Z?> bar(x: F, y: F?, z: Z, w: W) {
        foo1<F>(x)
        foo1(x)
        foo2<F>(x)
        foo2(x)

        foo1<<!UPPER_BOUND_VIOLATED!>F?<!>>(y)
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo1<!>(y)
        foo2<F?>(y)
        foo2(y)
        foo1<F>(<!TYPE_MISMATCH!>y<!>)
        foo2<F>(<!TYPE_MISMATCH!>y<!>)

        foo1<Z>(z)
        foo1(z)
        foo2<Z>(z)
        foo2(z)

        foo1<<!UPPER_BOUND_VIOLATED!>W<!>>(w)
        <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>foo1<!>(w)
        foo2<W>(w)
        foo2(w)
        foo1<<!UPPER_BOUND_VIOLATED!>W<!>>(w)
    }
}
