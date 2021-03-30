// FIR_IDENTICAL

interface Bar {
    fun <T> T.bar() {}
}

class A {
    companion object : Bar
}

fun test() {
    A.<error><error>bar</error></error>()
}
