// FIR_IDENTICAL
abstract class A

internal class B : A()

abstract class Base {
    protected abstract val a: A
}

internal class Derived : Base() {
    override val a = B()
        get() = field
}
