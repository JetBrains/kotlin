fun foo() {
    listOf(42).map { it }
    bar(A().b.toString())
    "hello".length
}

fun bar(x: Int) {
}

class A {
    val b: B = B()
}

class B {
    val c: Int = 42
}

fun usage() {
    data class A(val i: Int) {
        fun doSmth(): A = this
    }

    val a = A(10)
    a.doSmth().i
}

// SET_INT: METHOD_CALL_CHAIN_WRAP = 2
// SET_TRUE: WRAP_FIRST_METHOD_IN_CALL_CHAIN
