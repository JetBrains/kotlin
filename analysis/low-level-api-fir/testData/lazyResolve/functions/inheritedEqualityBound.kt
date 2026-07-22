// LANGUAGE: +StrictEquals

interface EB {
    override fun equals(@EqualityBound(EB::class) other: Any?): Boolean
}

class Inherited : EB {
    override fun equal<caret>s(other: Any?): Boolean = true
}
