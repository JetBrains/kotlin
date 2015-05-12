// C

interface Base<T> {
    fun foo(t: T): T
}

class C : Base<Unit> {
    override fun foo(t: Unit) {}
}