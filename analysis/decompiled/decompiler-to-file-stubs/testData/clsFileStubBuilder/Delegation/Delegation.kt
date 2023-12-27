interface T {
    fun f()
    val g: Int
}

class A() : T {
    override val g = 3
    override fun f() {
    }
}

class Delegation(val c: Int = 3, a: A) : T by a {
    fun ff(): Int = 3
}