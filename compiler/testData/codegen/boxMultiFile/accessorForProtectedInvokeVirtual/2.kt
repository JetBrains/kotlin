package test

abstract class A {
    public var state = ""

    // These implementations should not be called, because they are overridden in C

    protected open fun method(): String = "A.method"

    protected open var property: String
        get() = "A.property"
        set(value) { state += "A.property;" }
}
