// !LANGUAGE: +AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// IGNORE_BACKEND: JS

fun test(vararg s: String) = s[1]

fun box(): String =
    test(s = arrayOf("", "OK"))