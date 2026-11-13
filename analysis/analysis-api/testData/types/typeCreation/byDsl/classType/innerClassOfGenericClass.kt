class Outer<A> {
    inner class Inner<B>
}

fun foo(
    xx: Outer<Int>.Inner<String>,
    ww: Int,
    vv: String,
) {
    x<caret_type>x.toString()
    w<caret_int>w.toString()
    v<caret_string>v.toString()
}
