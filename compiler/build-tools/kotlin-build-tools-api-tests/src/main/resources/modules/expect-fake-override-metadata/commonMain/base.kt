open class A
class B : A()

open class Base {
    fun select(x: A): String = "common"
}

expect class Derived() : Base
