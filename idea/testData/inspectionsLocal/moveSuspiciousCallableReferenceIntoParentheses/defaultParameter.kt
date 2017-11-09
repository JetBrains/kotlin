// PROBLEM: Suspicious callable reference as the only lambda element
// WITH_RUNTIME

fun foo() {
    listOf(1,2,3).map {<caret> it::toString }
}