class Cell<T>(val x : T)
class Pair<T1, T2>(val x1: T1, val x2: T2)

typealias CIntA = Cell<Int>
typealias CA<TA> = Cell<TA>
typealias PIntIntA = Pair<Int, Int>
typealias PA<T1A, T2A> = Pair<T1A, T2A>
typealias P2A<TA> = Pair<TA, TA>

val test1 = CIntA(10)
val test2 = CA<Int>(10)
val test3 = CA(10)
val test4 = PIntIntA(10, 20)
val test5 = PA<Int, Int>(10, 20)
val test6 = PA(10, 20)
val test7 = P2A<Int>(10, 20)
val test8 = P2A(10, 20)

