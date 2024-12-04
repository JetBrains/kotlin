// LANGUAGE: +AllowAssigningArrayElementsToVarargsInNamedFormForFunctions

fun test(vararg s: String) = s[1] + s.size

fun box(): String {
    val r = test(s = arrayOf("aaa", "Bb"))

    if (r != "Bb2") return "fail: $r"

    return "OK"
}
