// "Surround with lambda" "true"
// ERROR: Type mismatch: inferred type is String? but String was expected
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference
fun nullableFn() {
    val nullableStr: String? = null
    str(<caret>nullableStr)
}

fun str(block: () -> String) {}