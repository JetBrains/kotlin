// PROBLEM: none
var a = 5

fun foo() = <caret>when {
    true -> a = 6
    else -> a = 8
}