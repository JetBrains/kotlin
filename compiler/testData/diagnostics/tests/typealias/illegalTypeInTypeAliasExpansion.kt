// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NullableNothingInReifiedPosition

typealias A<T> = Array<T>
typealias AA<T> = A<A<T>>

typealias AN = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>A<Nothing><!>
typealias ANN = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>A<Nothing?><!>

typealias AAN = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>AA<Nothing><!>
typealias AANN = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>AA<Nothing?><!>

typealias AAN2 = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>A<A<Nothing>><!>
typealias AANN2 = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>A<A<Nothing?>><!>

typealias First<X, Y> = List<X>
typealias UnusedAN1 = First<Int, <!UNSUPPORTED!>Array<Nothing><!>>
typealias UnusedAN2 = First<Int, A<Nothing>>
typealias UnusedANN1 = First<Int, <!UNSUPPORTED!>Array<Nothing?><!>>
typealias UnusedANN2 = First<Int, A<Nothing?>>

typealias Q<T> = Array<T?>
typealias QN = <!TYPEALIAS_EXPANDED_TO_MALFORMED_TYPE!>Q<Nothing><!>
