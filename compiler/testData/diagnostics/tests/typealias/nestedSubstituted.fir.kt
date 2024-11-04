// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Pair<T1, T2>(val x1: T1, val x2: T2)

class C<T> {
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias P2 = Pair<T, Int>
}

val p1: C<String>.P2 = <!TYPE_MISMATCH("T (of class C<T>); kotlin.String"), TYPE_MISMATCH("T (of class C<T>); kotlin.String")!>Pair("", 1)<!>
