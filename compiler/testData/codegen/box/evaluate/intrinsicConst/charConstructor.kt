// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB
fun <T> T.id() = this

const val char1 = Char(65)

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (char1.id() != 'A') return "Fail 1"

    return "OK"
}
