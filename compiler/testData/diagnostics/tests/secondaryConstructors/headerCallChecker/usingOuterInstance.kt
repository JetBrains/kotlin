// !DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(x: Outer) = 1
class Outer {
    inner class Inner {
        constructor(x: Int)
        constructor(x: Int, y: Int, z: Int = x + foo(this@Outer)) : this(x + foo(this@Outer))
    }
}
