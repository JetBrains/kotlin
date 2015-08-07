package aaa

enum class E {
    TRIVIAL_ENTRY,
    SUBCLASS { };

    class Nested {
        fun fortyTwo() = 42
    }
}
