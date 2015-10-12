interface IA {
    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>override fun toString(): String = "IA"<!>

    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>override fun equals(other: Any?): Boolean = <!SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE!>super<!>.equals(other)<!>

    <!METHOD_OF_ANY_IMPLEMENTED_IN_INTERFACE!>override fun hashCode(): Int {
        return 42;
    }<!>
}