// LANGUAGE: +StrictEquals

open class Base {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean = true
}

class Derived : Base() {
    // VIRTUAL_MEMBER_HIDDEN
    fun equ<caret>als(other: Any?): Boolean = true
}
