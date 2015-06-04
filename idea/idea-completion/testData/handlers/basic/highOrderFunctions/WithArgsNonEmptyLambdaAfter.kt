fun foo(p : (String, Char) -> Boolean){}

fun main(args: Array<String>) {
    fo<caret>{ x }
}

// ELEMENT: foo
// TAIL_TEXT: " { String, Char -> ... } (<root>)"
