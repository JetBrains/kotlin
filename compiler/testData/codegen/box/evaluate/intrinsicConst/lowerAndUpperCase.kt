// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB
fun <T> T.id() = this

const val lowercase1 = "lowercase".lowercase()
const val lowercase2 = "Lowercase".lowercase()
const val lowercase3 = "lowercasE".lowercase()

const val uppercase1 = "uppercase".uppercase()
const val uppercase2 = "Uppercase".uppercase()
const val uppercase3 = "uppercasE".uppercase()

const val lowerUpperCaseChained1 = "cHaInEd".lowercase().uppercase()
const val lowerUpperCaseChained2 = "cHaInEd".uppercase().lowercase()

fun box(): String {
    if (lowercase1.id() != "lowercase") return "Fail lowercase1"
    if (lowercase2.id() != "lowercase") return "Fail lowercase2"
    if (lowercase3.id() != "lowercase") return "Fail lowercase3"

    if (uppercase1.id() != "UPPERCASE") return "Fail uppercase1"
    if (uppercase2.id() != "UPPERCASE") return "Fail uppercase2"
    if (uppercase3.id() != "UPPERCASE") return "Fail uppercase3"

    if (lowerUpperCaseChained1.id() != "CHAINED") return "Fail lowerUpperCaseChained1"
    if (lowerUpperCaseChained2.id() != "chained") return "Fail lowerUpperCaseChained2"

    return "OK"
}
