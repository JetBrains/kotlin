// WITH_RUNTIME

class C {
    fun foo() {}
}

fun test() {
    val c = C()
    c.foo()<caret>
}