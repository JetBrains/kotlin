// LANGUAGE: +StrictEquals

open class Base {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean = true
}

class Derived : Base() {
    override fun equ<caret>als(other: Any?): Boolean = true
}
