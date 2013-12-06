package testing.rename

trait A {
    fun second() : Int
}

public open class B: A {
    override fun second() = 1

    fun first(a: Int) = 12
}

class C: B() {
    override fun second() = 2
}

fun usages() {
    val b = B()
    val a: A = b
    val c = C()

    a.second()
    b.second()
    c.second()
}


