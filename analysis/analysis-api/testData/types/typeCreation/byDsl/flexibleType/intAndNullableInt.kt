@Target(AnnotationTarget.TYPE)
annotation class Foo

@Target(AnnotationTarget.TYPE)
annotation class Bar

fun foo(xx: Int, yy: Int?) {
    x<caret_lower>x.toString()
    y<caret_upper>y.toString()
}