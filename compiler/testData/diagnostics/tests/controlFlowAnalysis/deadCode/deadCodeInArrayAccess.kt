// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testArrayAccess1(array: Array<Any>) {
    array<!UNREACHABLE_CODE!>[<!>todo()<!UNREACHABLE_CODE!>]<!>
}

fun testArrayAccess2() {
    operator fun Nothing.get(i: Int, s: String) {}
    todo()<!UNREACHABLE_CODE!>[1, ""]<!>
}

fun testAraryAccess3() {
    operator fun Nothing.get(n: Nothing) {}
    todo()<!UNREACHABLE_CODE!>[todo()]<!>
}

fun testArrayAssignment1(array: Array<Any>) {
    array[todo()] <!UNREACHABLE_CODE!>= 11<!>
}

fun testArrayAssignment2(array: Array<Any>) {
    array[1] <!UNREACHABLE_CODE!>=<!> todo()
}

fun testArrayAssignment3(n: Nothing) {
    operator fun Nothing.set(i: Int, j: Int) {}
    n<!UNREACHABLE_CODE!>[1] = 2<!>
}

fun testArrayAssignment4(n: Nothing) {
    operator fun Nothing.set(i: Int, a: Any) {}
    n<!UNREACHABLE_CODE!>[1] = todo()<!>
}

fun testArrayPlusAssign(array: Array<Any>) {
    operator fun Any.plusAssign(a: Any) {}
    array[1] <!UNREACHABLE_CODE!>+=<!> todo()
}

fun todo(): Nothing = throw Exception()