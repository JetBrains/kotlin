fun foo(p : (String, Char) -> Boolean){}

fun main(args: Array<String>) {
    fo<caret>
}

// ELEMENT: foo
// TAIL_TEXT: " { String, Char -> ... } (p: (String, Char) -> Boolean) (<root>)"
