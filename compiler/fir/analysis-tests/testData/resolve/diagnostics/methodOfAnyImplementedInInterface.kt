interface A {
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun toString() = "Hello"<!>
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun equals(other: Any?) = true<!>
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override fun hashCode(): Int {
        return 42;
    }<!>
}

interface B {
    override fun toString(): String
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

interface C {
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override operator fun toString(): String = "Rest"<!>
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override operator fun equals(other: Any?): Boolean = false<!>
    <!ANY_METHOD_IMPLEMENTED_IN_INTERFACE!>override operator fun hashCode(): Int = 2<!>
}

interface D {
    override operator fun toString(): String
    override operator fun equals(other: Any?): Boolean
    override operator fun hashCode(): Int
}