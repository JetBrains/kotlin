// IGNORE_BACKEND: JVM_IR

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

// 3 LDC "Hello,\\nWorld"
// 1 LDC "Hello,\\nHello,\\nWorld\\nWorld"
// 0 LDC "###"
// 0 INVOKESTATIC kotlin/text/StringsKt.trimMargin
