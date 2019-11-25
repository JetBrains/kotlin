// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// DONT_RUN_GENERATED_CODE: JS

fun escapeChar(c : Char) : String? = when (c) {
    '\\' -> "\\\\"
    '\n' -> "\\n"
    '"'  -> "\\\""
    else -> "" + c
}

tailrec fun String.escape(i : Int = 0, result : StringBuilder = StringBuilder()) : String =
        if (i == length) result.toString()
        else escape(i + 1, result.append(escapeChar(get(i))))

fun box() : String {
    "test me not \\".escape()
    return "OK"
}
