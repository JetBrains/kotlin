fun xfoo(option1: String = "", option2: Int = 1, p: () -> Unit){}

fun test(param: () -> Unit) {
    xfoo<caret>
}

// ELEMENT: xfoo
// TAIL_TEXT: " {...} (..., p: () -> Unit) (<root>)"

