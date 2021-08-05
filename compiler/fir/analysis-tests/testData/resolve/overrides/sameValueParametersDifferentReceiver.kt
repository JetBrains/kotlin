open class A {
    fun String.foo(from: String, to: String): Int {
        return 1
    }

    fun <T> T.foo(from: String, to: String): Int {
        return 1
    }
}

class B : A()
