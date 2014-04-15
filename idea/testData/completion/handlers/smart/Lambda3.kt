fun foo(p: (Int) -> Unit){}

fun bar() {
    foo(<caret>)
}

// ELEMENT: "{ Int -> ... }"
