open class Base {
    companion object {
        class Nested

        fun bar(): String = "Hello"
    }
    val classifierInBase = Companion.Nested()
    val callableInBase = Companion.bar()
}

class Child : Base() {
    val classifierInChild = Companion.Nested()
    val callableInChild = Companion.bar()
}