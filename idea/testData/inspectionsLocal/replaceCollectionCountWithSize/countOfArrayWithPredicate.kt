// PROBLEM: none
// WITH_RUNTIME

fun foo() {
    var array = arrayOf(1,2,3)
    array.<caret>count { i -> i == 1 }
}
