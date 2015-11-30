package a

fun foo() {
    <caret>assert(true, { "text" })
}

class AssertionError

// WITH_RUNTIME
