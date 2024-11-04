// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Pair<T1, T2>(val x1: T1, val x2: T2)

class C<T> {
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias P2 = Pair<T, T>
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias PT2<T2> = Pair<T, T2>

    fun first(p: P2) = p.x1
    fun second(p: P2) = p.x2

    fun <T2> first2(p: PT2<T2>) = p.x1
    fun <T2> second2(p: PT2<T2>) = p.x2
}

val p1 = Pair(1, 1)
val p2 = Pair(1, "")

val test1: Int = C<Int>().first(<!ARGUMENT_TYPE_MISMATCH("C.P2; Pair<kotlin.Int, kotlin.Int>")!>p1<!>)
val test2: Int = C<Int>().second(<!ARGUMENT_TYPE_MISMATCH("C.P2; Pair<kotlin.Int, kotlin.Int>")!>p1<!>)

val test3: Int = C<Int>().<!CANNOT_INFER_PARAMETER_TYPE!>first2<!>(<!ARGUMENT_TYPE_MISMATCH("C.PT2<T2 (of fun <T2> first2)>; Pair<kotlin.Int, kotlin.String>")!>p2<!>)
val test4: String = C<Int>().second2(<!ARGUMENT_TYPE_MISMATCH("C.PT2<T2 (of fun <T2> second2)>; Pair<kotlin.Int, kotlin.String>")!>p2<!>)
