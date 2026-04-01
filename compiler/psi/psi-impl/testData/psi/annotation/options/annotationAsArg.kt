class Annotation(val x: Int) {
    fun baz() {}
    fun bar() = x
}

fun foo(annotation: Annotation): Int {
    if (annotation.bar() == 0) {
        annotation.baz()
        return 0
    }
    else {
        return -1
    }
}
