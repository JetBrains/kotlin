enum class EnumEntry {
    X {
        operator fun equals(a: Int, b: Int): Boolean = true
    }, Y;
}
