// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class Pair<T1, T2>(val x1: T1, val x2: T2)

class C {
    typealias P2 = Pair<Int, Int>

    fun p() = <!UNRESOLVED_REFERENCE!>P2<!>(1, 1)
    fun first(p: <!UNRESOLVED_REFERENCE!>P2<!>) = p.<!UNRESOLVED_REFERENCE!>x1<!>
    fun second(p: <!UNRESOLVED_REFERENCE!>P2<!>) = p.<!UNRESOLVED_REFERENCE!>x2<!>
}

val p1 = Pair(1, 1)

val test1: Int = C().first(p1)
val test2: Int = C().second(p1)

fun C.testExtFun1(x: C.P2) = x

fun C.testExtFun2(): C.P2 {
    val x: C.P2 = p()
    val y = C.<!UNRESOLVED_REFERENCE!>P2<!>(1, 1)
    return x
}
