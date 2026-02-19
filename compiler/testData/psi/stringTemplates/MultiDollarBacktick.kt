// COMPILATION_ERRORS

fun parserBug() {
    // No closing backtick — not an identifier
    """
        ${"${"$`identifier"}"} "}"}
    """.trimIndent()

    // Two backticks — everything inside is an identifier
    """
        ${"${"$`identifier"}"} `"}"}
    """.trimIndent()

    // Escaped dollar, not an identifier
    """
        ${"${"\$`identifier"}"} `"}"}
    """.trimIndent()

    // Innermost string should not grab too much
    """
        ${"${$$$"$$`identifier"}"} `"}"}
    """.trimIndent()

    // Three dollars is the escape sequence, everything inside the backticks is an identifier
    """
        ${"${$$$"$$$`identifier"}"} `"}"}
    """.trimIndent()

    // Without the closing backtick the innermost string stops where expected
    """
        ${"${$$$"$$`identifier"}"} "}"}
    """.trimIndent()

    // on simple strings
    $$$"$$`identifier"
    "${$$$"$$`identifier"}
    "${$$$"$$$`identifier`"}
}