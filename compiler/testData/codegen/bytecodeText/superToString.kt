interface A

abstract class Base: A

class Derived : Base() {
    override fun toString() = super.toString()
}

// 1 INVOKESPECIAL Base.toString