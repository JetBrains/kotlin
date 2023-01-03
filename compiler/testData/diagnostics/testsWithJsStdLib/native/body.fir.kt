external fun foo(): Int = definedExternally

external fun bar(): Unit {
    definedExternally
}

external fun baz(): Int = 23

external fun f(x: Int, y: String = definedExternally): Unit

external fun g(x: Int, y: String = ""): Unit

external var a: Int
    get() = definedExternally
    set(value) {
        definedExternally
    }

external val b: Int
    get() = 23

external val c: Int = definedExternally

external val d: Int = 23

external class C {
    fun foo(): Int = definedExternally

    fun bar(): Int = 23
}
