fun bar(f: (Int, String, Int, String) -> Unit) {
}

fun main() {
    bar<caret>
}

// ELEMENT: bar
// TAIL_TEXT: " { Int, String, Int, String -> ... } (f: (Int, String, Int, String) -> Unit) (<root>)"