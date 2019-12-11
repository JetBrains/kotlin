fun testAssignment() {
    var a = 1
    a = todo()
}

fun testVariableDeclaration() {
    val a = todo()
}

fun testPlusAssign() {
    operator fun Int.plusAssign(i: Int) {}

    var a = 1
    a += todo()
}


fun todo(): Nothing = throw Exception()