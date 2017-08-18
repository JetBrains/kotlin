// IS_APPLICABLE: false

fun foo(handler: () -> Unit) { }

fun bar() {
    foo { <caret>zoo() }
}

fun zoo(){}