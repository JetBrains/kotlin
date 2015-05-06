fun foo(s: String){ }

fun getString(i: Int): String?{}

fun bar() {
    foo(<caret>)
}

// ELEMENT_TEXT: "?: getString"
// TAIL_TEXT: "(i: Int) (<root>)"
