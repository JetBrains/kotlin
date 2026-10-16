// KT-90199
data class MyPair(val i: Int, val b: Int)

val pair = MyPair(1, 2)

val (first, la<caret>st) = pair
