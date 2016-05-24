class Pair<T1, T2>(val x1: T1, val x2: T2)

typealias P2<T> = Pair<T, T>

val test1 = P2<String>("", "")