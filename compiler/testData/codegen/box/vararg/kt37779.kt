// !LANGUAGE: +AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS

fun test(vararg s: String) = s[1]

fun box(): String =
    test(s = arrayOf("", "OK"))