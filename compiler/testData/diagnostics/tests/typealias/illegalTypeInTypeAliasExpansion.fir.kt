typealias A<T> = Array<T>
typealias AA<T> = A<A<T>>

typealias AN = <!TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS!>A<Nothing><!>

typealias AAN = <!TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS!>AA<Nothing><!>

typealias AAN2 = <!TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS!>A<A<Nothing>><!>

typealias First<X, Y> = List<X>
typealias UnusedAN1 = First<Int, Array<Nothing>>
typealias UnusedAN2 = First<Int, A<Nothing>> // TODO
