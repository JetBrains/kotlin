// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^^^ KT-74942: Extra annotation @IntrinsicConstEvaluation appeared on fake override `E.name()`

interface Named {
    val name: String
}

enum class E : Named {
    OK
}

fun box(): String {
    return E.OK.name
}
