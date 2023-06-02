// IGNORE_BACKEND_K2: JVM_IR

class SimpleClass(val s: String) {
    fun foo() = s
}

val rv = SimpleClass("OK").foo()

// expected: rv: OK
