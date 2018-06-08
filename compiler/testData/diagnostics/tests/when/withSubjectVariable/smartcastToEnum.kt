// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE

enum class E { FIRST, SECOND }

fun testSmartcastToEnumInSubjectInitializer(e: E?) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (val ne = e!!) { // TODO fix
        E.FIRST -> "f"
        E.SECOND -> "s"
    }
}


sealed class Either
class Left : Either()
class Right : Either()

fun testSmartcastToSealedInSubjectInitializer(x: Any?) {
    val y = <!NO_ELSE_IN_WHEN!>when<!> (val either = x as Either) { // TODO fix
        is Left -> "L"
        is Right -> "R"
    }
}