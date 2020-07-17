// MODE: property
enum class E {
    ENTRY;
    companion object {
        val test: E = ENTRY
    }
}

val test<# : E# > = E.test
            