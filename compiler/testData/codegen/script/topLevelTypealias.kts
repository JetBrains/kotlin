
class SimpleClass(val s: String) {
    fun foo() = s
}

typealias Test = SimpleClass

val rv = Test("OK").foo()

// expected: rv: OK
