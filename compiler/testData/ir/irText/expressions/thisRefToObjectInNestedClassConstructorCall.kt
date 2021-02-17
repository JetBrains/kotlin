// FIR_IDENTICAL
open class Base(val x: Any)

object Host {
    class Derived1 : Base(this)
    class Derived2 : Base(Host)
}