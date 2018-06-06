class GClass<T> {
    fun foo(t: T): T {
        return t
    }
}

val g = GClass<Int>()
g.foo(1)