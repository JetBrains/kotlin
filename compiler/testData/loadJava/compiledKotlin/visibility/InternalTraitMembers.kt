package test

interface A {
    internal fun f() : Int
    internal val v : Int
    public var p : Int
        internal set
}
