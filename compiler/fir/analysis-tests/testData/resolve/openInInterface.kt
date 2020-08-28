interface Some {
    open fun foo()
    open fun bar() {}

    open val x: Int
    open val y = 1
    open val z get() = 1

    open var xx: Int
    open var yy = 1
    open var zz: Int
        set(value) {
            field = value
        }
}