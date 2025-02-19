// LANGUAGE: -ForbidParenthesizedLhsInAssignments
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

class Cell(var x: Int) {
    operator fun get(i: Int) = x
    operator fun set(i: Int, v: Int) { x = v }
}

fun box(): String {
    val c = Cell(0)
    (c[0])++
    if (c[0] != 1) return "Fail"
    return "OK"
}