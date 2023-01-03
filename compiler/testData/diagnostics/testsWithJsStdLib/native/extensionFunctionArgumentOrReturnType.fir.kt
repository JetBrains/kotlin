external fun foo(f: Int.() -> Int)

external fun bar(vararg f: Int.() -> Int)

external fun baz(): Int.() -> Int

external val prop: Int.() -> Int

external var prop2: Int.() -> Int

external val propGet
    get(): Int.() -> Int = definedExternally

external var propSet
    get(): Int.() -> Int = definedExternally
    set(v: Int.() -> Int) = definedExternally

external class A(f: Int.() -> Int)

external data class B(
        val a: Int.() -> Int,
        var b: Int.() -> Int
) {
    val c: Int.() -> Int
}
