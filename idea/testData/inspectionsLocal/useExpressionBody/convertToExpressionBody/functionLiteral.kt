// PROBLEM: none

fun foo(handler: () -> Unit) { }

fun bar() {
    foo { <caret>zoo() }
}

fun zoo(){}