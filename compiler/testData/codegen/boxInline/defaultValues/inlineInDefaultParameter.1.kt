import test.*

fun testCompilation(arg: String = getStringInline()): String {
    return arg
}

inline fun testCompilationInline(arg: String = getStringInline()): String {
    return arg
}

fun box(): String {
    var result = testCompilation()
    if (result != "OK") return "fail1: ${result}"

    result = testCompilation("OKOK")
    if (result != "OKOK") return "fail2: ${result}"


    result = testCompilationInline()
    if (result != "OK") return "fail3: ${result}"

    result = testCompilationInline("OKOK")
    if (result != "OKOK") return "fail4: ${result}"

    return "OK"
}