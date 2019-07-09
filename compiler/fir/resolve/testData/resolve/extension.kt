fun String.foo() {}

fun String.bar() {
    foo()
}

class My {
    fun bar() {
        foo()
    }
}

fun My.foo() {}