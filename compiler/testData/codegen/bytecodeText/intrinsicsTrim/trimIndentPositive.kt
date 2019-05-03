// IGNORE_BACKEND: JVM_IR

fun constant(): String {
    return """
        Hello,
        World
    """.trimIndent()
}

private const val HAS_INDENT = """Hello,
        World"""
fun interpolatedUsingConstant(): String {
    return """
        Hello,
        $HAS_INDENT
        World
    """.trimIndent()
}

// 1 LDC "Hello,\\nWorld"
// 1 LDC "Hello,\\nHello,\\nWorld\\nWorld"
// 0 INVOKESTATIC kotlin/text/StringsKt.trimIndent
