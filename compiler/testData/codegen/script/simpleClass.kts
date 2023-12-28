
class SimpleClass(val s: String) {
    fun foo() = s
}

val rv = SimpleClass("OK").foo()

// expected: rv: OK
