// COMPILER_ARGUMENTS: -XXLanguage:+MixedNamedArgumentsInTheirOwnPosition
fun foo(s: String, b: Boolean) {}

fun bar() {
    foo(s = "", <caret>b = true)
}