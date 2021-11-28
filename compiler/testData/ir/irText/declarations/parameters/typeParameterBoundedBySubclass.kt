// FIR_IDENTICAL
abstract class Base1<T : Derived1>
class Derived1 : Base1<Derived1>()

abstract class Base2 {
    fun <T : Derived2> foo(x: T) {}
}
class Derived2 : Base2()
