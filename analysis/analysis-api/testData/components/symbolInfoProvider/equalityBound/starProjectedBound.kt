// LANGUAGE: +StrictEquals

class Generic<T> {
    override fun equ<caret>als(@EqualityBound(Generic::class) other: Any?): Boolean = true
}
