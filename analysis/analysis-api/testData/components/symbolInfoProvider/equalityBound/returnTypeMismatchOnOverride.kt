// LANGUAGE: +StrictEquals

open class Base {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean = true
}

class Derived : Base() {
    // RETURN_TYPE_MISMATCH_ON_OVERRIDE
    override fun equ<caret>als(other: Any?): String = ""
}
