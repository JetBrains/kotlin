fun foo(p: (word: String, number: Int) -> Unit){}

fun bar() {
    foo(<caret>)
}

// ELEMENT: "{ word, number -> ... }"
