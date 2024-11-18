// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +NestedTypeAliases

class Pair<X, Y>(val x: X, val y: Y)
class Triple<X, Y, Z>(val x: X, val y: Y, val z: Z)
class D<T>

class C<T> {
    inner typealias P = Pair<T, T>

    inner typealias P1<X> = Pair<X, T>
    inner typealias P2<Y> = Pair<T, Y>

    inner typealias TripleTA<X, Y> = Triple<T, X, Y>

    inner typealias P3<Z> = P1<Z>

    inner typealias TA = D<T>
    inner typealias TA2 = C<T>

    fun test() {
        P<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>(0, 0) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
        P(0, 0) // OK

        P1<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int><!>("str1", 1) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
        P1<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int, Char><!>("str1", 1) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
        P1<String>("str1", 1) // OK
        P1<String>(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>"str1"<!>) // ARGUMENT_TYPE_MISMATCH
        P1("str1", 1) // OK

        P2<String>(<!ARGUMENT_TYPE_MISMATCH!>"str2"<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>) // ARGUMENT_TYPE_MISMATCH
        P2<String>(2, "str2") // OK
        P2(2, "str2") // OK

        TripleTA<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>(3, "str3", <!ARGUMENT_TYPE_MISMATCH!>'c'<!>) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
        TripleTA<String, Char>(3, "str3", 'c') // OK
        TripleTA(3, "str3", 'c') // OK

        P3("str4", 4) // OK
        P3<String>("str4", 4) // OK
        P3<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, String><!>("str4", 4) // WRONG_NUMBER_OF_TYPE_ARGUMENTS

        TA() // OK
        TA2() // OK

        InnerTA() // OK
        InnerTA2<String>() // OK

        Inner()
        InnerWithTypeParam<String>()
    }
}

fun test() {
    val c = C<Int>()

    c.P<String>(<!ARGUMENT_TYPE_MISMATCH!>0<!>, <!ARGUMENT_TYPE_MISMATCH!>0<!>) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
    c.P(0, 0) // OK

    c.P1<String, Int>("str1", 1) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
    c.P1<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int, Char><!>("str1", 1) // WRONG_NUMBER_OF_TYPE_ARGUMENTS
    c.P1<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>("str1", 1) // OK
    c.P1<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>(<!ARGUMENT_TYPE_MISMATCH!>1<!>, "str1") // ARGUMENT_TYPE_MISMATCH
    c.P1("str1", 1) // OK

    c.P2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>("str2", <!ARGUMENT_TYPE_MISMATCH!>2<!>) // ARGUMENT_TYPE_MISMATCH
    c.P2<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>(2, "str2") // OK
    c.P2(2, "str2") // OK

    c.TripleTA<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>(3, "str3", 'c') // WRONG_NUMBER_OF_TYPE_ARGUMENTS
    c.TripleTA<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Char><!>(3, "str3", 'c') // OK
    c.TripleTA(3, "str3", 'c') // OK

    c.P3("str4", 4) // OK
    c.P3<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>("str4", 4) // OK
    c.P3<String, String>("str4", <!ARGUMENT_TYPE_MISMATCH!>4<!>) // WRONG_NUMBER_OF_TYPE_ARGUMENTS

    c.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>TA<!>() // OK
    c.<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>TA2<!>() // OK
}
