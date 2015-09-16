open class Base {
    open val x: Int = 1
        get() = field - 1
}

class Other: Base() {
    override val x = 2
}

class Another: Base() {
    override val x = 3
        get() = field + 1
}

class NoBackingField: Base() {
    override val x: Int
        get() = 5
}