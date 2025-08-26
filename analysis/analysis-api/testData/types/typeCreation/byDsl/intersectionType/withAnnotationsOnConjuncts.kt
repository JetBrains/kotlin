annotation class MyAnno1
annotation class MyAnno2
annotation class MyAnno3


fun foo(xx: Int, yy: String) {
    x<caret_1>x.toString()
    y<caret_2>y.toString()
}