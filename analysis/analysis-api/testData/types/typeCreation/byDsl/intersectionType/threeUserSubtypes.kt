interface A
interface B: A
interface C: B

fun foo(xx: A, yy: B, zz: C) {
    x<caret_1>x.toString()
    y<caret_2>y.toString()
    z<caret_3>z.toString()
}