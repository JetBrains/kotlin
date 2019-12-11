class Pair<T1, T2>(val x1: T1, val x2: T2)

typealias P2<T> = Pair<T, T>

val test1: Pair<String, String> = P2<String>("", "")
val test1x1: String = test1.x1
val test1x2: String = test1.x2

val test2: P2<String> = P2<String>("", "")
val test2x1: String = test2.x1
val test2x2: String = test2.x2
