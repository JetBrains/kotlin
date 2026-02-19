interface A

class B: A

fun foo(xx: B, yy: A) {
    x<caret_lower>x.toString()
    y<caret_upper>y.toString()
}