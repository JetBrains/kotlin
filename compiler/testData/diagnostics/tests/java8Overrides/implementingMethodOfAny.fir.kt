interface IA {
    override fun toString(): String = "IA"

    override fun equals(other: Any?): Boolean = super.equals(other)

    override fun hashCode(): Int {
        return 42;
    }
}