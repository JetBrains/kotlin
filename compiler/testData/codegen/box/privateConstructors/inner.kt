// See also KT-6299
public open class Outer private constructor(val s: String) {
    inner class Inner: Outer("O") {
        fun foo(): String {
            return this.s + this@Outer.s
        }
    }
    class Nested: Outer("K") 
    fun bar() = Inner()
}

fun box(): String {
    val inner = Outer.Nested().bar()
    return inner.foo()
}