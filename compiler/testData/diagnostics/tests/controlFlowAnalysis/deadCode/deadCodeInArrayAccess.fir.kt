// !DIAGNOSTICS: -UNUSED_PARAMETER

fun testArrayAccess1(array: Array<Any>) {
    array[todo()]
}

fun testArrayAccess2() {
    operator fun Nothing.get(i: Int, s: String) {}
    todo()[1, ""]
}

fun testAraryAccess3() {
    operator fun Nothing.get(n: Nothing) {}
    todo()[todo()]
}

fun testArrayAssignment1(array: Array<Any>) {
    array[todo()] = 11
}

fun testArrayAssignment2(array: Array<Any>) {
    array[1] = todo()
}

fun testArrayAssignment3(n: Nothing) {
    operator fun Nothing.set(i: Int, j: Int) {}
    n[1] = 2
}

fun testArrayAssignment4(n: Nothing) {
    operator fun Nothing.set(i: Int, a: Any) {}
    n[1] = todo()
}

fun testArrayPlusAssign(array: Array<Any>) {
    operator fun Any.plusAssign(a: Any) {}
    array[1] += todo()
}

fun todo(): Nothing = throw Exception()