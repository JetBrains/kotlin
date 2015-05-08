enum class E {
    ENTRY;

    companion object {
        fun entry() = ENTRY
    }
}

fun bar() = E.entry()
