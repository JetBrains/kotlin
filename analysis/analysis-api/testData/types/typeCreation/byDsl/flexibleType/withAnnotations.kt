annotation class MyAnno1
annotation class MyAnno2
annotation class MyAnno3

fun foo(xx: Any, yy: Any?) {
    x<caret_lower>x.toString()
    y<caret_upper>y.toString()
}
