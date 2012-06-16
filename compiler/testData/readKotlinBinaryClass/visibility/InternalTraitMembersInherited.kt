package test

trait A {
    internal open fun f() : Int = 0
    internal open val v : Int
        get() = 0
    public var p : Int
        get() = 5
        internal set(value) {
        }
}

class B : A {
}
