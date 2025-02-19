// LANGUAGE: -ForbidParenthesizedLhsInAssignments
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

fun box(): String {
    var x = 1
    (foo@ x)++
    ++(foo@ x)

    if (x != 3) return "Fail: $x"
    return "OK"
}
