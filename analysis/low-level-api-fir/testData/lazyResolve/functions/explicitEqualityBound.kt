// LANGUAGE: +StrictEquals

interface EB {
    override fun eq<caret>uals(@EqualityBound(EB::class) other: Any?): Boolean
}
