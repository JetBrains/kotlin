typealias A<T> = Array<T>
typealias AA<T> = A<A<T>>

typealias AN = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>A<Nothing><!>

typealias AAN = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>AA<Nothing><!>

typealias AAN2 = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>A<A<Nothing>><!>

typealias First<X, <!UNUSED_TYPEALIAS_PARAMETER!>Y<!>> = List<X>
typealias UnusedAN1 = First<Int, <!UNSUPPORTED!>Array<Nothing><!>>
typealias UnusedAN2 = First<Int, A<Nothing>> // TODO
