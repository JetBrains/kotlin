class Name {
    default object {
        fun create(): Name = Name()
    }
}

fun foo(name: Name){}

fun bar() {
    val v: (Name) -> Unit = { foo(<caret>) }
}

// ORDER: it
// ORDER: create
// ORDER: Name
