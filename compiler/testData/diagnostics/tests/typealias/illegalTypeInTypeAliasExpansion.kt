typealias A<T> = Array<T>
typealias AA<T> = A<A<T>>

typealias AN = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>A<Nothing><!>

typealias AAN = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>AA<Nothing><!>
