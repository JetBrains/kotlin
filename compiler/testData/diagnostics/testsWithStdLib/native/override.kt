import kotlin.jvm.*

trait Base {
    fun foo()
}

class Derived : Base {
    override native fun foo()
}