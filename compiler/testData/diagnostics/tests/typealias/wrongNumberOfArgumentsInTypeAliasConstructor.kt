// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class Pair<T1, T2>(val x1: T1, val x2: T2)

typealias P<T1, T2> = Pair<T1, T2>

typealias P2<T> = Pair<T, T>

typealias PR<T1, T2> = Pair<T2, T1>

val test0 = P(1, 2)
val test1 = P<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>(1, 2)
val test2 = P<Int, Int>(1, 2)
val test3 = P<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>(1, 2)

val test0p2 = P2(1, 1)
val test0p2a = P2(1, "")
val test1p2 = P2<Int>(1, 1)
val test2p2 = P2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>(1, 1)
val test3p2 = P2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int, Int><!>(1, 1)

val test0pr = PR(1, "")
val test1pr = PR<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>(1, <!OI;TYPE_MISMATCH!>""<!>)
val test2pr = PR<Int, String>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, <!TYPE_MISMATCH!>""<!>)
val test2pra = PR<String, Int>(1, "")
val test3pr = P2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int, Int><!>(<!OI;CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, "")

class Num<T : Number>(val x: T)
typealias N<T> = Num<T>

val testN0 = <!OI;TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>N<!>(<!NI;TYPE_MISMATCH!>""<!>)
val testN1 = N<Int>(1)
val testN1a = N<<!UPPER_BOUND_VIOLATED!>String<!>>("")
val testN2 = N<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int, Int><!>(1)

class MyPair<T1 : CharSequence, T2 : Number>(val string: T1, val number: T2)
typealias MP<T1> = MyPair<String, T1>

val testMP0 = MP<Int>("", 1)
val testMP1 = <!OI;TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>MP<!>(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, <!NI;TYPE_MISMATCH!>""<!>)
val testMP2 = MP<<!UPPER_BOUND_VIOLATED!>String<!>>("", "")