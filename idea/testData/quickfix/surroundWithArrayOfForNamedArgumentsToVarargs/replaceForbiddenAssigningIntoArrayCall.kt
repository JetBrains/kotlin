// "Surround with *arrayOf(...)" "true"
// COMPILER_ARGUMENTS: -XXLanguage:+ProhibitAssigningSingleElementsToVarargsInNamedForm
// DISABLE-ERRORS

fun anyFoo(vararg a: Any) {}

fun test() {
    anyFoo(a = in<caret>tArrayOf(1))
}