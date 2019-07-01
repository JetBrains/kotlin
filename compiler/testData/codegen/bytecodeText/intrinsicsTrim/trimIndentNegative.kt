fun notConstant(arg: String): String {
    return arg.trimIndent()
}

fun interpolated(arg: Int):String {
    return """
        Hello,
        $arg
    """.trimIndent()
}

fun notInvoked():String {
    return """
        Hello,
        World
    """
}

// 1 LDC "\\n        Hello,\\n        World\\n    "
// 2 INVOKESTATIC kotlin/text/StringsKt.trimIndent
