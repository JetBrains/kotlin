package dependency

class MyPair<A, B>(public val first: A, public val second: B)

infix fun <A, B> A.pair(that: B): MyPair<A, B> = MyPair(this, that)
