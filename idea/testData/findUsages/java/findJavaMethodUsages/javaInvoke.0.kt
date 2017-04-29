fun f(c: JavaClassInvoke) {
    c()
}

fun foo(o: JavaClassInvoke.OtherJavaClass) {
    o()
    JavaClassInvoke.OtherJavaClass.OJC()
}

fun foo() {
    JavaClassInvoke.INSTANCE()
    JavaClassInvoke.AnotherOther.INSTANCE()
    JavaClassInvoke.JavaOther.INSTANCE()
}
