fun f(c: JavaClass) {
    c()
}

fun foo(o: JavaClass.OtherJavaClass) {
    o()
    JavaClass.OtherJavaClass.OJC()
}