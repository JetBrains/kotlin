// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -NullableNothingInReifiedPosition

typealias A<T> = Array<T>
typealias AA<T> = A<A<T>>

typealias AN = <!TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS!>A<Nothing><!>
typealias ANN = <!TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS!>A<Nothing?><!>

typealias AAN = <!TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS!>AA<Nothing><!>
typealias AANN = <!TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS!>AA<Nothing?><!>

typealias AAN2 = <!TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS!>A<A<Nothing>><!>
typealias AANN2 = <!TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS!>A<A<Nothing?>><!>

typealias First<X, Y> = List<X>
typealias UnusedAN1 = First<Int, Array<Nothing>>
typealias UnusedAN2 = First<Int, A<Nothing>>
typealias UnusedANN1 = First<Int, Array<Nothing?>>
typealias UnusedANN2 = First<Int, A<Nothing?>>

typealias Q<T> = Array<T?>
typealias QN = <!TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS!>Q<Nothing><!>
