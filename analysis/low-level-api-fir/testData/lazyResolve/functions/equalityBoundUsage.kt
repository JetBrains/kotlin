// LANGUAGE: +StrictEquals

fun test(eb: EB, a: Any?): Boolean {
    re<caret>turn eb == a
}

interface EB {
    override fun equals(@EqualityBound(EB::class) other: Any?): Boolean
}
