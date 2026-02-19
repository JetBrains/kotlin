fun foo(xx: Nothing, yy: String) {
    x<caret_lower1>x.toString()
    y<caret_upper1>y.toString()
}

fun bar(xx: Int, yy: Any?) {
    x<caret_lower2>x.toString()
    y<caret_upper2>y.toString()
}