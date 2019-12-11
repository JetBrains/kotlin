typealias A<T> = Array<T>
typealias AA<T> = A<A<T>>

typealias AN = A<Nothing>

typealias AAN = AA<Nothing>

typealias AAN2 = A<A<Nothing>>

typealias First<X, Y> = List<X>
typealias UnusedAN1 = First<Int, Array<Nothing>>
typealias UnusedAN2 = First<Int, A<Nothing>> // TODO
