// LANGUAGE: +StrictEquals

class A {
    fun so<caret>mething(@EqualityBound(A::class) other: Any?): Boolean = true
}
