// LANGUAGE: +StrictEquals

class A {
    fun equ<caret>als(@EqualityBound(A::class) other: String): Boolean = true
}
