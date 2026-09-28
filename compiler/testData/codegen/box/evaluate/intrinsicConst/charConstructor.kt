// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB
fun <T> T.id() = this

const val x = 66

const val char1 = Char(65)
const val char2 = Char(x)

fun box(): String {
    if (char1.id() != 'A') return "Fail 1"
    if (char2.id() != 'B') return "Fail 2"

    return "OK"
}
