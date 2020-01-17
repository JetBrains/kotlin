// COMPILER_ARGUMENTS: -XXLanguage:+MixedNamedArgumentsInTheirOwnPosition
fun foo(name1: Int, name2: Int, name3: Int) {}

fun usage() {
    foo(1, name2 = 2, <caret>name3 = 3)
}