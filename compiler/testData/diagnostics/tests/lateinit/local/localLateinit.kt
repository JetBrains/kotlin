// FIR_IDENTICAL
// !LANGUAGE: +LateinitLocalVariables

fun test() {
    lateinit var s: String
    s = ""
    s.length
}