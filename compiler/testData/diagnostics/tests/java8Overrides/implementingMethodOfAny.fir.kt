interface IA {
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun toString(): String = "IA"<!>

    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun equals(other: Any?): Boolean = super.equals(other)<!>

    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun hashCode(): Int {
        return 42;
    }<!>
}