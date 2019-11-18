// !LANGUAGE: +VariableDeclarationInWhenSubject
// IGNORE_BACKEND_FIR: JVM_IR

var effectCount = 0

fun withSideEffect(): Any {
    effectCount++
    return 42
}

fun box(): String {
    when (val y = withSideEffect()) {
        1 -> throw AssertionError()
        "" -> throw AssertionError()
        is String -> throw AssertionError()
        42 -> {}
    }

    if (effectCount != 1) throw AssertionError("effectCount=$effectCount")

    return "OK"
}