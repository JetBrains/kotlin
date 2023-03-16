// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// WITH_STDLIB

const val trimIndent = "123".trimIndent()
const val complexTrimIndent =
    """
            ABC
            123
            456
        """.trimIndent()

fun box(): String {
    if (trimIndent != "123") return "Fail 1"
    if (complexTrimIndent != "ABC\n123\n456") return "Fail 2"
    return "OK"
}