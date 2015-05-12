import kotlin.jvm.*

interface Base {
    fun foo()
}

class Derived : Base {
    override native fun foo()
}