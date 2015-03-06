enum class E {
    ENTRY

    default object {
        fun entry() = ENTRY
    }
}

fun bar() = E.entry()
