class Outer<A1, A2> {
    class Middle<B1, B2> {
        inner class Inner<C1, C2>
    }
}

fun foo(
    xx: Outer.Middle<Boolean, Char>.Inner<Long, Double>,
    ww: Boolean,
    vv: Char,
    uu: Long,
    tt: Double,
) {
    x<caret_type>x.toString()
    w<caret_boolean>w.toString()
    v<caret_char>v.toString()
    u<caret_long>u.toString()
    t<caret_double>t.toString()
}
