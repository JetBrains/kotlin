class SimpleClass(val s: String) {
    fun foo() = s
}

SimpleClass("OK").foo()

// expected: rv: OK
