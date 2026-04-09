fun constant(): String {
    return """
        |Hello,
        |World
    """.trimMargin()
}

private const val HAS_MARGIN = """Hello,
        |World"""
fun interpolatedUsingConstant(): String {
    return """
        |Hello,
        |$HAS_MARGIN
        |World
    """.trimMargin()
}


private const val SPACES = "    "
private const val HELLO = "Hello"
private const val WORLD = "World"
fun reliesOnNestedStringBuilderFlatteningAndConstantConcatenation(): String {
    return ("" + '\n' + SPACES + "${SPACES}|Hey" + """
        |${HELLO + HELLO},
        |${WORLD + WORLD}
""" + SPACES).trimMargin()
}

fun constantCustomPrefix(): String {
    return """
        ###Hello,
        ###World
    """.trimMargin(marginPrefix = "###")
}

private const val OCTOTHORPE = '#'
fun constantCustomPrefixInterpolatedUsingConstant(): String {
    return """
        #@#Hello,
        #@#World
    """.trimMargin(marginPrefix = "$OCTOTHORPE@$OCTOTHORPE")
}

// 0 LDC "Hello,\\nWorld"
// 0 LDC "Hello,\\nHello,\\nWorld\\nWorld"
// 0 LDC "Hey\\nHelloHello,\\nWorldWorld"
// 1 LDC "###"
// 5 INVOKESTATIC kotlin/text/StringsKt.trimMargin
