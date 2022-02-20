// MODULE: m1-common
// FILE: common.kt

interface Foo {
    fun ok(x: Int, y: String = "")

    fun failX(x: Int, y: String = "")

    fun failY(x: Int, y: String = "")
}

expect class Bar : Foo {
    override fun ok(x: Int, y: String)

    override fun failX(x: Int, y: String)

    override fun failY(x: Int, y: String)
}

fun test(foo: Foo, bar: Bar) {
    foo.ok(42)
    foo.ok(42, "OK")
    bar.ok(42)
    bar.ok(42, "OK")
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Bar : Foo {
    actual override fun ok(x: Int, y: String) {}
    
    actual override fun failX(x: Int = 0, y: String) {}

    actual override fun failY(x: Int, y: String = "") {}
}
