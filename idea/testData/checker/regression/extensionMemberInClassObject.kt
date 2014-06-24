trait Bar {
    fun <T> T.bar() {}
}

class A {
    class object : Bar
}

fun test() {
    A.<error><error>bar</error></error>()
}