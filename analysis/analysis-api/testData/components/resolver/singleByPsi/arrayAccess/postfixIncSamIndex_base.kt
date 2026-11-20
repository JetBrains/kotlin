fun interface IFoo {
    fun foo(i: Int)
}

class B {
    operator fun get(i: IFoo): Int = 0
    operator fun set(i: IFoo, value: Int) {}
}

fun test(b: B, fn: (Int) -> Unit) {
    <expr>b[fn]</expr>++
}
