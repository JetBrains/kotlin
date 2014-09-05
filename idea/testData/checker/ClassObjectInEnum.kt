enum class E {
    ENTRY

    class object {
        fun foo(): E = ENTRY
    }
}

fun foo() = E.ENTRY
fun bar() = E.values()
fun baz() = E.valueOf("ENTRY")
