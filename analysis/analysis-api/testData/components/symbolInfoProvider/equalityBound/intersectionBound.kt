// LANGUAGE: +StrictEquals

interface Left {
    override fun equals(@EqualityBound(Left::class) other: Any?): Boolean
}

interface Right {
    override fun equals(@EqualityBound(Right::class) other: Any?): Boolean
}

class Combined : Left, Right {
    override fun equ<caret>als(other: Any?): Boolean = true
}
