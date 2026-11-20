class Outer<A> {
    class Nested<B>
}

fun foo(
    xx: Outer.Nested<String>,
    vv: String,
) {
    x<caret_type>x.toString()
    v<caret_string>v.toString()
}
