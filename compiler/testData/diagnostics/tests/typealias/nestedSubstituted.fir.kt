// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Pair<T1, T2>(val x1: T1, val x2: T2)

class C<T> {
    typealias P2 = Pair<T, Int>
}

val p1: C<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>.P2 = Pair("", 1)
