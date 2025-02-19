// LANGUAGE: -ForbidParenthesizedLhsInAssignments
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature

fun box(): String {
    var x = 1
    (foo@ x)++
    ++(foo@ x)

    if (x != 3) return "Fail: $x"
    return "OK"
}
