enum class E {
    ENTRY;

    override val name: String = "lol"
    override val ordinal: Int = 0

    override fun compareTo(other: E) = -1

    override fun equals(other: Any?) = true
    override fun hashCode() = -1
}
