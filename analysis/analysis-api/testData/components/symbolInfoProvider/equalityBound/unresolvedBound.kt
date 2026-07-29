// LANGUAGE: +StrictEquals

class A {
    override fun equ<caret>als(@EqualityBound(Missing::class) other: Any?): Boolean = true
}
