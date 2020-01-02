// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE

sealed class Either
class Left : Either()
class Right : Either()

fun testSmartcastToSealedInSubjectInitializer1(x: Any?) {
    val y1 = when (val either = x as Either) {
        is Left -> "L"
        is Right -> "R"
    }
}

fun testSmartcastToSealedInSubjectInitializer2(x: Any?) {
    val y2 = when (val either: Any = x as Either) { // NB explicit type annotation
        is Left -> "L"
        is Right -> "R"
    }
}