// FIX: Generate equals & hashCode by identity

abstract class Base {
    open val prop: Int
        get() = 13
}

sealed class SC : Base() {
    <caret>class U : SC()

    override val prop: Int
        get() = 42
}