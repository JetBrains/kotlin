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

private const val SPACES = "    "
private const val HELLO = "Hello"
private const val WORLD = "World"
fun reliesOnNestedStringBuilderFlatteningAndConstantConcatenation(): String {
    return ("" + '\n' + SPACES + "${SPACES}Hey" + """
        ${HELLO + HELLO},
        ${WORLD + WORLD}
""" + SPACES).trimIndent()
}

// 1 LDC "Hello,\\nWorld"
// 1 LDC "Hello,\\nHello,\\nWorld\\nWorld"
// 1 LDC "Hey\\nHelloHello,\\nWorldWorld"
// 0 INVOKESTATIC kotlin/text/StringsKt.trimIndent
