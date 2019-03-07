fun notConstant(arg: String): String {
    return arg.trimMargin()
}

fun notConstantCustomPrefix(arg: String): String {
    return arg.trimMargin("###")
}

fun interpolated(arg: Int):String {
    return """
        |Hello,
        |$arg
    """.trimMargin()
}

fun notInvoked():String {
    return """
        |Hello,
        |World
    """
}

fun constantWithNonConstantCustomPrefix(arg: String): String {
    return """
        |Hello,
        |World
    """.trimMargin(arg)
}

// 1 LDC "\\n        \|Hello,\\n        \|"
// 1 LDC "\\n    "
// 2 LDC "\\n        \|Hello,\\n        \|World\\n    "
// 4 INVOKESTATIC kotlin/text/StringsKt.trimMargin
