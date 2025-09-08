interface A
class B: A

fun foo(xx: B, yy: A, zz: Int) {
    x<caret_lower>x.toString()
    y<caret_upper>y.toString()
    z<caret_type>z.toString()
}