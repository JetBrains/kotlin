fun foo() = throw Exception()

fun bar() = null!!

fun baz() = bar()

fun gav(): Any = null!!

val x = null!!

val y: Nothing = throw Exception()

fun check() {
    // Error: KT-10449
    fun local() = bar()
    // Unreachable / unused, but not implicit Nothing
    val x = null!!
}

fun nonLocalReturn() = run { <!RETURN_TYPE_MISMATCH!>return<!> }

class Klass {
    fun bar() = null!!

    val y = null!!

    init {
        fun local() = bar()
        // Should be unreachable: see KT-5311
        val z = null!!
    }

    fun foo() {
        fun local() = bar()

        val x = y
    }
}

interface Base {
    val x: Int

    fun foo(): String
}

class Derived : Base {
    // Ok for override

    override val x = null!!

    override fun foo() = null!!
}
