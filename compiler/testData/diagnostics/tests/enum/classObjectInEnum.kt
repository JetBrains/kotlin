enum class E {
    ENTRY

    class object {
        fun entry() = ENTRY
    }
}

fun bar() = E.entry()
