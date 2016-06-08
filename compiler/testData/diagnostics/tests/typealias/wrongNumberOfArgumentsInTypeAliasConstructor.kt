class Pair<T1, T2>(val x1: T1, val x2: T2)

typealias P<T1, T2> = Pair<T1, T2>

typealias P2<T> = Pair<T, T>

typealias PR<T1, T2> = Pair<T2, T1>

val test0 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>P<!>(1, 2)
val test1 = P<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>(1, 2)
val test2 = P<Int, Int>(1, 2)
val test3 = P<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>(1, 2)

val test0p2 = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>P2<!>(1, 1)
val test1p2 = P2<Int>(1, 1)
val test2p2 = P2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>(1, 1)
val test3p2 = P2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>(1, 1)

val test0pr = <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>PR<!>(1, "")
val test1pr = PR<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>(1, <!TYPE_MISMATCH!>""<!>)
val test2pr = PR<Int, String>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, <!TYPE_MISMATCH!>""<!>)
val test2pra = PR<String, Int>(1, "")
val test3pr = P2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int, Int><!>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, "")
