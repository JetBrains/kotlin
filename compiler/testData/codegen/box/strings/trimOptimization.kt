// WITH_STDLIB
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: WASM

import kotlin.test.assertEquals

// CHECK_CONTAINS_NO_CALLS: trimIndentConstant
fun trimIndentConstant(): String {
    return """
        Hello,
        World
    """.trimIndent()
}

// CHECK_CONTAINS_NO_CALLS: trimIndentInterpolatedUsingConstant
private const val HAS_INDENT = """Hello,
        World"""
fun trimIndentInterpolatedUsingConstant(): String {
    return """
        Hello,
        $HAS_INDENT
        World
    """.trimIndent()
}

// CHECK_CONTAINS_NO_CALLS: trimIndentReliesOnNestedStringBuilderFlatteningAndConstantConcatenation
private const val SPACES = "    "
private const val HELLO = "Hello"
private const val WORLD = "World"
fun trimIndentReliesOnNestedStringBuilderFlatteningAndConstantConcatenation(): String {
    return ("" + '\n' + SPACES + "${SPACES}Hey" + """
        ${HELLO + HELLO},
        ${WORLD + WORLD}
""" + SPACES).trimIndent()
}

// CHECK_CALLED_IN_SCOPE: function=trimIndent scope=trimIndentNotConstant
fun trimIndentNotConstant(arg: String): String {
    return arg.trimIndent()
}

// CHECK_CALLED_IN_SCOPE: function=trimIndent scope=trimIndentInterpolated
fun trimIndentInterpolated(arg: Int): String {
    return """
        Hello,
        $arg
    """.trimIndent()
}

// CHECK_CONTAINS_NO_CALLS: trimMarginConstant
fun trimMarginConstant(): String {
    return """
        |Hello,
        |World
    """.trimMargin()
}

// CHECK_CONTAINS_NO_CALLS: trimMarginInterpolatedUsingConstant
private const val HAS_MARGIN = """Hello,
        |World"""
fun trimMarginInterpolatedUsingConstant(): String {
    return """
        |Hello,
        |$HAS_MARGIN
        |World
    """.trimMargin()
}

// CHECK_CONTAINS_NO_CALLS: trimMarginReliesOnNestedStringBuilderFlatteningAndConstantConcatenation
fun trimMarginReliesOnNestedStringBuilderFlatteningAndConstantConcatenation(): String {
    return ("" + '\n' + SPACES + "${SPACES}|Hey" + """
        |${HELLO + HELLO},
        |${WORLD + WORLD}
""" + SPACES).trimMargin()
}

// CHECK_CONTAINS_NO_CALLS: trimMarginConstantCustomPrefix
fun trimMarginConstantCustomPrefix(): String {
    return """
        ###Hello,
        ###World
    """.trimMargin(marginPrefix = "###")
}

// CHECK_CONTAINS_NO_CALLS: trimMarginConstantCustomPrefixInterpolatedUsingConstant
private const val OCTOTHORPE = '#'
fun trimMarginConstantCustomPrefixInterpolatedUsingConstant(): String {
    return """
        #@#Hello,
        #@#World
    """.trimMargin(marginPrefix = "$OCTOTHORPE@$OCTOTHORPE")
}

// CHECK_CALLED_IN_SCOPE: function=trimMargin$default scope=trimMarginNotConstant
fun trimMarginNotConstant(arg: String): String {
    return arg.trimMargin()
}

// CHECK_CALLED_IN_SCOPE: function=trimMargin scope=trimMarginNotConstantCustomPrefix
fun trimMarginNotConstantCustomPrefix(arg: String): String {
    return arg.trimMargin("###")
}

// CHECK_CALLED_IN_SCOPE: function=trimMargin$default scope=trimMarginInterpolated
fun trimMarginInterpolated(arg: Int): String {
    return """
        |Hello,
        |$arg
    """.trimMargin()
}

// CHECK_CALLED_IN_SCOPE: function=trimMargin scope=trimMarginConstantWithNonConstantCustomPrefix
fun trimMarginConstantWithNonConstantCustomPrefix(arg: String): String {
    return """
        |Hello,
        |World
    """.trimMargin(arg)
}

fun box(): String {

    assertEquals("Hello,\nWorld", trimIndentConstant())
    assertEquals("Hello,\nHello,\nWorld\nWorld", trimIndentInterpolatedUsingConstant())
    assertEquals("Hey\nHelloHello,\nWorldWorld", trimIndentReliesOnNestedStringBuilderFlatteningAndConstantConcatenation())
    assertEquals("Hello,\nWorld", trimIndentNotConstant("""
        Hello,
        World
    """))
    assertEquals("Hello,\n42", trimIndentInterpolated(42))

    assertEquals("Hello,\nWorld", trimMarginConstant())
    assertEquals("Hello,\nHello,\nWorld\nWorld", trimMarginInterpolatedUsingConstant())
    assertEquals("Hey\nHelloHello,\nWorldWorld", trimMarginReliesOnNestedStringBuilderFlatteningAndConstantConcatenation())
    assertEquals("Hello,\nWorld", trimMarginConstantCustomPrefix())
    assertEquals("Hello,\nWorld", trimMarginConstantCustomPrefixInterpolatedUsingConstant())
    assertEquals("Hello,\nWorld", trimMarginNotConstant("""
        |Hello,
        |World
    """))
    assertEquals("Hello,\nWorld", trimMarginNotConstantCustomPrefix("""
        ###Hello,
        ###World
    """))
    assertEquals("Hello,\n42", trimMarginInterpolated(42))
    assertEquals("Hello,\nWorld", trimMarginConstantWithNonConstantCustomPrefix("|"))

    return "OK"
}
