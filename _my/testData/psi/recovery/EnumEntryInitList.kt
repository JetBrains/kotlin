enum class E1 {
    FIRST:
}

enum class E2 {
    FIRST:

    fun some() {}
}

enum class E3 {
    FIRST:

    val some = 1
}

enum class E4 {
    FIRST:

    class Other
}

enum class E5 {
    FIRST: E5(),

    class Other
}

enum class E6 {
    FIRST: E6(),;

    class Other
}

enum class E7 {
    FIRST:

    [Some]
    SECOND
}

enum class E8 {
    FIRST:
    SECOND
}