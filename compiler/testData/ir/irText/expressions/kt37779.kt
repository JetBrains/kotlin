// FIR_IDENTICAL
// LANGUAGE: +AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
fun foo(vararg s: String) {}

fun test1() {
    foo(s = arrayOf("", "OK"))
}

fun test2(ss: Array<String>) {
    foo(s = ss)
}