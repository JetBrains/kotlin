trait Bar {
    fun <T> T.bar() {}
}

class A {
    default object : Bar
}

fun test() {
    A.<error><error>bar</error></error>()
}