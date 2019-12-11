abstract class Base {
    final override fun equals(other: Any?) = false
    final override fun hashCode() = 42

    open override fun toString() = "OK"
}

data class Data1(val field: String) : Base()

interface AbstractAnyMembers {
    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
    abstract override fun toString(): String
}

data class Data2(val field: String): Base(), AbstractAnyMembers
