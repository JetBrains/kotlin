interface My {
    open fun foo()
    open fun bar() {}
    <!REDUNDANT_MODIFIER!>open<!> abstract fun baz(): Int

    open val x: Int
    open val y: String
        get() = ""
    <!REDUNDANT_MODIFIER!>open<!> abstract val z: Double
}
