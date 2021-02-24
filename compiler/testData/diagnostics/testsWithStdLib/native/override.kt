// FIR_IDENTICAL
import kotlin.jvm.*

interface Base {
    fun foo()
}

class Derived : Base {
    override external fun foo()
}